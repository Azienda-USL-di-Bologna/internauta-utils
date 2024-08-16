package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.Versamento.StatoVersamento;
import it.bologna.ausl.model.entities.versatore.VersamentoAllegato;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import javax.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Classe del thread che richiama il versamento vero è proprio
 *
 * @author gdm
 */
public class VersatoreDocThread implements Callable<List<VersamentoDocInformation>> {

    private final List<VersamentoDocInformation> vesamentiDoc;
//    private final AzioneVersamento azioneVersamento;
    private final VersatoreDocs versatoreDocsInstance;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final SessioneVersamento sessioneVersamento;
    private final Integer idPersonaForzatura;
    private static final Logger log = LoggerFactory.getLogger(VersatoreDocThread.class);

    /**
     * Costruisce il thread che effettua la chiamata del versamento
     *
     * @param vesamentiDoc contiene una lista di versamenti che il thread
     * effettuerà. Sono tutti i versamenti per lo stesso doc
     * @param sessioneVersamento la sessione alla quale attaccare il versamento
     * @param idPersonaForzatura l'id della persona che effettua la forzatura,
     * se non è una forzatura passare null
     * @param versatoreDocsInstance l'istanza del versatore dell'azienda
     * @param transactionTemplate
     * @param entityManager
     */
    public VersatoreDocThread(List<VersamentoDocInformation> vesamentiDoc, SessioneVersamento sessioneVersamento, Integer idPersonaForzatura, VersatoreDocs versatoreDocsInstance, TransactionTemplate transactionTemplate, EntityManager entityManager) {
        this.vesamentiDoc = vesamentiDoc;
        this.sessioneVersamento = sessioneVersamento;
        this.versatoreDocsInstance = versatoreDocsInstance;
        this.transactionTemplate = transactionTemplate;
        this.entityManager = entityManager;
        this.idPersonaForzatura = idPersonaForzatura;
    }

    /**
     * effettua la chiamata al versatore specifico per tutti i versamenti
     * passati nel parametro vesamentiDoc
     *
     * @return la lista dei versamenti con i dati risultato popolati dal
     * versatore specifico
     * @throws Exception
     */
    @Override
    public List<VersamentoDocInformation> call() throws Exception {
        if (vesamentiDoc != null && !vesamentiDoc.isEmpty()) {
            for (VersamentoDocInformation versamentoDocInformation : vesamentiDoc) {
                // se lo stato del versamento non è ERRORE, allora richiamo il versamento sul plugin
                if (versamentoDocInformation.getStatoVersamentoPrecedente() != StatoVersamento.ERRORE) {
                    try {
                        versamentoDocInformation = versatoreDocsInstance.versa(versamentoDocInformation);
                    } catch (Throwable ex) {
                        log.error("errore nel versamento", ex);
                        versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                        versamentoDocInformation.setCodiceErrore("INTERNAL");
                        versamentoDocInformation.setDescrizioneErrore("Errore tecnico durante il processo di versamento");
                    }
                    JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
                    ZonedDateTime now = ZonedDateTime.now();
                    insertVersamentoAndUpdateAllegatiAndDoc(queryFactory, versamentoDocInformation.getIdDoc(), versamentoDocInformation, idPersonaForzatura, now);
                } else {
                    /*
                    se lo stato del versamento era ERRORE, allora setto come stato ERRORE (in modo che ne tenga conto nel calcolo dello stato finale
                     */
                    versamentoDocInformation.setStatoVersamento(versamentoDocInformation.getStatoVersamentoPrecedente());
                }
            }
        }
        return vesamentiDoc;
    }

    /**
     * per ogni versamento effettuato, viene salvato il versamento sul DB. Salva
     * il versamento del doc, il versamento degli allegati. Inoltre sul doc e
     * sugli allegati viene aggiornato lo stato di ultimo versamento e settato a
     * null lo stato del prossimo versamento (colonna stato_versamento)
     *
     * @param queryFactory
     * @param idDoc il doc del versamento
     * @param versamentoDocInformation oggetto con le informazioni del
     * versamento
     * @param personaForzatura la persona che effettua la forzatura, se non è
     * una forzatura passare null
     * @param now data da inserire come data di versamento
     */
    private void insertVersamentoAndUpdateAllegatiAndDoc(JPAQueryFactory queryFactory, Integer idDoc, VersamentoDocInformation versamentoDocInformation, Integer idPersonaForzatura, ZonedDateTime now) {
        transactionTemplate.executeWithoutResult(a -> {
            Doc doc = entityManager.find(Doc.class, versamentoDocInformation.getIdDoc());
            Archivio archivio = null;
            if (versamentoDocInformation.getIdArchivio() != null) {
                archivio = entityManager.find(Archivio.class, versamentoDocInformation.getIdArchivio());
            }

            Persona personaForzatura = getPersonaForzatura();
            // prima crea il versamento e lo salva
            Versamento versamento = buildVersamento(versamentoDocInformation, doc, personaForzatura, archivio, now);
            entityManager.persist(versamento);
            Versamento.StatoVersamento statoVersamentoDoc = versamentoDocInformation.getStatoVersamento();
            /* 
            per ogni allegato salva il versamento_allegato e aggiorna lo stato ultimo versamento sull'allegato
            inoltre svuoto lo stato prossimo versamento (stato_versamento)
             */
            boolean forzabile = versamentoDocInformation.getForzabile();
            boolean forzabileConcordato = versamentoDocInformation.getForzabileConcordato();
            if (versamentoDocInformation.getVersamentiAllegatiInformations() != null) {
                for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentoDocInformation.getVersamentiAllegatiInformations()) {
                    Allegato allegato = entityManager.find(Allegato.class, versamentoAllegatoInformation.getIdAllegato());
                    Allegato.DettaglioAllegato dettaglioAllegato = allegato.getDettagli().getByKey(versamentoAllegatoInformation.getTipoDettaglioAllegato());
                    dettaglioAllegato.setStatoVersamento(null);
                    dettaglioAllegato.setStatoUltimoVersamento(versamentoAllegatoInformation.getStatoVersamento());
                    dettaglioAllegato.setVersamentoForzabile(versamentoAllegatoInformation.getForzabile());

                    /*
                    per ogni allegato calcola lo stato versamento del doc.
                    Lo stato cambia a seconda dello stato dell'allegato
                     */
                    if (statoVersamentoDoc != StatoVersamento.ANNULLATO) {
                        statoVersamentoDoc = getStatoVersamentoDocFromStatoVersamentoAllegato(statoVersamentoDoc, versamentoAllegatoInformation.getStatoVersamento());
                    }
                    VersamentoAllegato versamentoAllegato = buildVersamentoAllegato(versamentoAllegatoInformation, versamento, allegato, now);
                    forzabile = forzabile || versamentoAllegatoInformation.getForzabile();
                    entityManager.persist(allegato);
                    entityManager.persist(versamentoAllegato);
                }
                versamentoDocInformation.setStatoVersamento(statoVersamentoDoc);
                versamentoDocInformation.setForzabile(forzabile);
            }
            // alla fine scrive lo stato del doc e la forzabilità (calcolati in base agli allegati) sul versamento
            versamento.setStato(statoVersamentoDoc);
            versamento.setForzabile(forzabile);
            versamento.setForzabileConcordato(forzabileConcordato);

            /* 
            se il versamento è andato a buon fine, oppure è stato annullato (lo stato è VERSATO, o ANNULLATO), 
            allora il versamento viene settato da ignorare
             */
            if (statoVersamentoDoc == Versamento.StatoVersamento.VERSATO || statoVersamentoDoc == Versamento.StatoVersamento.ANNULLATO) {
                versamento.setIgnora(true);
            }

            // viene salvato il versamento e il doc, inoltre viene settato a null lo stato del prossimo versamento (stato_versamento)
            entityManager.persist(versamento);
            doc.setStatoVersamento(null);
            entityManager.persist(doc);

            // viene settato lo stato ultimo versamento sul doc
            queryFactory
                    .update(QDocDetail.docDetail)
                    .set(QDocDetail.docDetail.statoUltimoVersamento, statoVersamentoDoc != null ? statoVersamentoDoc : null)
                    .set(QDocDetail.docDetail.dataUltimoVersamento, now)
                    .set(QDocDetail.docDetail.versamentoForzabile, forzabile)
                    .set(QDocDetail.docDetail.versamentoForzabileConcordato, forzabileConcordato)
                    .where(QDocDetail.docDetail.id.eq(idDoc))
                    .execute();

            /* 
            se questo versamento è attaccato a un versamento precedente, viene settato da ignorare il versamento precedente,
            perché dal prossimo giro verrà usato questo versamento
             */
            if (versamentoDocInformation.getIdVersamentoPrecedente() != null) {
                queryFactory
                        .update(QVersamento.versamento)
                        .set(QVersamento.versamento.ignora, true)
                        .where(QVersamento.versamento.id.eq(versamentoDocInformation.getIdVersamentoPrecedente()))
                        .execute();
            }
        });
    }

    /**
     * Reperisce l'entità Persona che forza il versamento
     *
     * @return l'entità Persona che forza il versamento, oppure null se non è
     * stata passata dei dati del job
     */
    private Persona getPersonaForzatura() {
        if (idPersonaForzatura != null) {
            return entityManager.find(Persona.class, idPersonaForzatura);
        }
        return null;
    }

    /**
     * Crea l'entità VersamentoAllegato per poterla salvare sl DB
     *
     * @param versamentoAllegatoInformation oggetto contenente tutti i dati del
     * versamento dell'allegato
     * @param versamento il versamento al quale attavvare il VersamentoAllegato
     * @param allegato l'allegato versato
     * @param now la data da settare sul VersamentoAllegato
     * @return l'entità VersamentoAllegato per poterla salvare sl DB
     */
    private VersamentoAllegato buildVersamentoAllegato(VersamentoAllegatoInformation versamentoAllegatoInformation, Versamento versamento, Allegato allegato, ZonedDateTime now) {
        VersamentoAllegato versamentoAllegato = new VersamentoAllegato();
        versamentoAllegato.setCodiceErrore(versamentoAllegatoInformation.getCodiceErrore());
        versamentoAllegato.setDescrizioneErrore(versamentoAllegatoInformation.getDescrizioneErrore());
        versamentoAllegato.setDettaglioAllegato(versamentoAllegatoInformation.getTipoDettaglioAllegato());
        versamentoAllegato.setRapporto(versamentoAllegatoInformation.getRapporto());
        versamentoAllegato.setMetadatiVersati(versamentoAllegatoInformation.getMetadatiVersati());
        versamentoAllegato.setStato(versamentoAllegatoInformation.getStatoVersamento());
        versamentoAllegato.setForzabile(versamentoAllegatoInformation.getForzabile());
        versamentoAllegato.setIdVersamento(versamento);
        versamentoAllegato.setIdAllegato(allegato);
        versamentoAllegato.setDataInserimento(now);

        return versamentoAllegato;
    }

    /**
     * Crea il Versamento per poterlo salvare sul DB. Se è presente un
     * versamento precedente lo setta.
     *
     * @param versamentoDocInformation oggetto contenente tutti i dati del
     * versamento
     * @param doc il doc al quale il versamento è attaccato
     * @param personaForzatura la persona che ha forzato il versamento (null se
     * non è una forzatura)
     * @param archivio il fascicolo al quale il doc è legato (null se non è
     * legato a nessun fascicolo)
     * @param now la data del versamento
     * @return l'entità Versamento per poterla salvare sul DB
     */
    private Versamento buildVersamento(VersamentoDocInformation versamentoDocInformation, Doc doc, Persona personaForzatura, Archivio archivio, ZonedDateTime now) {
        Versamento versamento = new Versamento();
        if (versamentoDocInformation.getIdVersamentoPrecedente() != null) {
            Versamento versamentoPrecedente = entityManager.find(Versamento.class, versamentoDocInformation.getIdVersamentoPrecedente());
            versamento.setIdVersamentoRitentato(versamentoPrecedente);
        }
        versamento.setCodiceErrore(versamentoDocInformation.getCodiceErrore());
        versamento.setDescrizioneErrore(versamentoDocInformation.getDescrizioneErrore());
        versamento.setData(versamentoDocInformation.getDataVersamento());
        if (versamentoDocInformation.getTipologiaVersamento() == SessioneVersamento.TipologiaVersamento.FORZATURA) {
            versamento.setDataForzatura(versamentoDocInformation.getDataVersamento());
        }
        versamento.setIdDoc(doc);
        versamento.setIdArchivio(archivio);
        versamento.setIdPersonaForzatura(personaForzatura);
        //sessioneVersamento = entityManager.find(SessioneVersamento.class, sessioneVersamento.getId());
        //facciamo in questo modo perché altrementi si verifica un errore, in quanto la sessioneVersamento risulta detached quando si esegue la persistenza del versamento
        SessioneVersamento sessioneVersamento1 = entityManager.find(SessioneVersamento.class, sessioneVersamento.getId());
        versamento.setIdSessioneVersamento(sessioneVersamento1);
        versamento.setMetadatiVersati(versamentoDocInformation.getMetadatiVersati());
        versamento.setRapporto(versamentoDocInformation.getRapporto());
        versamento.setStato(versamentoDocInformation.getStatoVersamento());
        versamento.setDataInserimento(now);

        return versamento;
    }

    /**
     * Calcola lo stato del versamento in base al versamento dell'allegato
     *
     * @param statoVersamentoDocAttuale lo stato del versamento attuale
     * @param statoVersamentoAllegato lo stato del versamento dell'allegato
     * @return lo stato del versamento in base al versamento dell'allegato
     */
    private StatoVersamento getStatoVersamentoDocFromStatoVersamentoAllegato(StatoVersamento statoVersamentoDocAttuale, StatoVersamento statoVersamentoAllegato) {
        if (statoVersamentoDocAttuale == null) {
            statoVersamentoDocAttuale = statoVersamentoAllegato;
        } else {
            switch (statoVersamentoAllegato) {
                case ERRORE:
                    switch (statoVersamentoDocAttuale) {
                        case IN_CARICO:
                            statoVersamentoDocAttuale = StatoVersamento.IN_CARICO_CON_ERRORI;
                            break;
                        case VERSATO:
                            statoVersamentoDocAttuale = StatoVersamento.ERRORE;
                            break;
                    }
                    break;
                case ERRORE_RITENTABILE:
                    switch (statoVersamentoDocAttuale) {
                        case IN_CARICO:
                            statoVersamentoDocAttuale = StatoVersamento.IN_CARICO_CON_ERRORI_RITENTABILI;
                            break;
                        case VERSATO:
                        case ERRORE:
                            statoVersamentoDocAttuale = StatoVersamento.ERRORE_RITENTABILE;
                            break;
                    }
                    break;
                case VERSARE:
                    switch (statoVersamentoDocAttuale) {
                        case ERRORE:
                        case VERSATO:
                            statoVersamentoDocAttuale = StatoVersamento.ERRORE_RITENTABILE;
                            break;
                    }
                    break;
                case IN_CARICO:
                    switch (statoVersamentoDocAttuale) {
                        case ERRORE:
                            statoVersamentoDocAttuale = StatoVersamento.IN_CARICO_CON_ERRORI;
                            break;
                        case ERRORE_RITENTABILE:
                            statoVersamentoDocAttuale = StatoVersamento.IN_CARICO_CON_ERRORI_RITENTABILI;
                            break;
                        case VERSARE:
                        case VERSATO:
                            statoVersamentoDocAttuale = StatoVersamento.IN_CARICO;
                            break;
                    }
                    break;
            }
        }

        return statoVersamentoDocAttuale;
    }
}
