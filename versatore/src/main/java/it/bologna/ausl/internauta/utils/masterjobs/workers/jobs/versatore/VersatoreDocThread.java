package it.bologna.ausl.internauta.utils.masterjobs.workers.jobs.versatore;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.versatore.VersamentoAllegatoInformation;
import it.bologna.ausl.internauta.utils.versatore.VersamentoDocInformation;
import it.bologna.ausl.internauta.utils.versatore.VersatoreDocs;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.scripta.Allegato;
import it.bologna.ausl.model.entities.scripta.Archivio;
import it.bologna.ausl.model.entities.scripta.Doc;
import it.bologna.ausl.model.entities.scripta.QDocDetail;
import it.bologna.ausl.model.entities.versatore.QVersamento;
import it.bologna.ausl.model.entities.versatore.SessioneVersamento;
import it.bologna.ausl.model.entities.versatore.Versamento;
import it.bologna.ausl.model.entities.versatore.VersamentoAllegato;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import javax.persistence.EntityManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author gdm
 */
public class VersatoreDocThread implements Callable<List<VersamentoDocInformation>> {

    
    private final List<VersamentoDocInformation> vesamentiDoc;
    private final VersatoreDocs versatoreDocsInstance;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final SessioneVersamento sessioneVersamento;
    private final Persona personaForzatura;

    public VersatoreDocThread(List<VersamentoDocInformation> vesamentiDoc, SessioneVersamento sessioneVersamento, Persona personaForzatura, VersatoreDocs versatoreDocsInstance, TransactionTemplate transactionTemplate, EntityManager entityManager) {
        this.vesamentiDoc = vesamentiDoc;
        this.sessioneVersamento = sessioneVersamento;
        this.versatoreDocsInstance = versatoreDocsInstance;
        this.transactionTemplate = transactionTemplate;
        this.entityManager = entityManager;
        this.personaForzatura = personaForzatura;
    }
    
    
    @Override
    public List<VersamentoDocInformation> call() throws Exception {
        if (vesamentiDoc != null && !vesamentiDoc.isEmpty()) {
            for (VersamentoDocInformation versamentoDocInformation : vesamentiDoc) {
                try {
                    versamentoDocInformation = versatoreDocsInstance.versa(versamentoDocInformation);
                } catch (Throwable ex) {
                    versamentoDocInformation.setStatoVersamento(Versamento.StatoVersamento.ERRORE);
                }
                JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
                ZonedDateTime now = ZonedDateTime.now();
                updateStatoDoc(queryFactory, versamentoDocInformation.getIdDoc(), versamentoDocInformation, personaForzatura, now);
            }
        }
        return vesamentiDoc;
    }
    
    private void updateStatoDoc(JPAQueryFactory queryFactory, Integer idDoc, VersamentoDocInformation versamentoDocInformation, Persona personaForzatura, ZonedDateTime now) {       
        transactionTemplate.executeWithoutResult(a -> {
            Doc doc = entityManager.find(Doc.class, versamentoDocInformation.getIdDoc());
            Archivio archivio = null;
            if (versamentoDocInformation.getIdArchivio() != null) {
                archivio = entityManager.find(Archivio.class, versamentoDocInformation.getIdArchivio());
            }
            Versamento versamento = buildVersamento(queryFactory, versamentoDocInformation, doc, personaForzatura, archivio, now);
            entityManager.persist(versamento);
            Versamento.StatoVersamento statoVersamentoDoc = versamentoDocInformation.getStatoVersamento();
            if (versamentoDocInformation.getVeramentiAllegatiInformations() != null) {
                for (VersamentoAllegatoInformation versamentoAllegatoInformation : versamentoDocInformation.getVeramentiAllegatiInformations()) {
                    Allegato allegato = entityManager.find(Allegato.class, versamentoAllegatoInformation.getIdAllegato());
                    Allegato.DettaglioAllegato dettaglioAllegato = allegato.getDettagli().getByKey(versamentoAllegatoInformation.getTipoDettaglioAllegato());
                    dettaglioAllegato.setStatoVersamento(null);
                    dettaglioAllegato.setStatoUltimoVersamento(versamentoAllegatoInformation.getStatoVersamento());
                    statoVersamentoDoc = getStatoVersamentoDocFromStatoVersamentoAllegato(statoVersamentoDoc, versamentoAllegatoInformation.getStatoVersamento());
                    VersamentoAllegato versamentoAllegato = buildVersamentoAllegato(queryFactory, versamentoAllegatoInformation, versamento, allegato, now);
                    entityManager.persist(allegato);
                    entityManager.persist(versamentoAllegato);
                }
                versamentoDocInformation.setStatoVersamento(statoVersamentoDoc);
            }
            versamento.setStato(statoVersamentoDoc);
            entityManager.persist(versamento);
            doc.setStatoVersamento(null);
            entityManager.persist(doc);
            
            queryFactory
                .update(QDocDetail.docDetail)
                .set(QDocDetail.docDetail.statoUltimoVersamento, statoVersamentoDoc != null ? statoVersamentoDoc.toString(): null)
                .set(QDocDetail.docDetail.dataUltimoVersamento, now)
                .where(QDocDetail.docDetail.id.eq(idDoc))
                .execute();
            if (versamentoDocInformation.getVersamentoPrecedente() != null) {
            queryFactory
                .update(QVersamento.versamento)
                .set(QVersamento.versamento.ignora, true)
                .where(QVersamento.versamento.id.eq(versamentoDocInformation.getVersamentoPrecedente()))
                .execute();
            }
        });
    }
    
    private VersamentoAllegato buildVersamentoAllegato(JPAQueryFactory queryFactory, VersamentoAllegatoInformation versamentoAllegatoInformation, Versamento versamento, Allegato allegato, ZonedDateTime now) {
        VersamentoAllegato versamentoAllegato = new VersamentoAllegato();
        versamentoAllegato.setCodiceErrore(versamentoAllegatoInformation.getCodiceErrore());
        versamentoAllegato.setDescrizioneErrore(versamentoAllegatoInformation.getDescrizioneErrore());
        versamentoAllegato.setDettaglioAllegato(versamentoAllegatoInformation.getTipoDettaglioAllegato());
        versamentoAllegato.setStato(versamentoAllegatoInformation.getStatoVersamento());
        versamentoAllegato.setIdVersamento(versamento);
        versamentoAllegato.setIdAllegato(allegato);
        versamentoAllegato.setDataInserimento(now);
        
        return versamentoAllegato;
    }
    
    private Versamento buildVersamento(JPAQueryFactory queryFactory, VersamentoDocInformation versamentoDocInformation, Doc doc, Persona personaForzatura, Archivio archivio, ZonedDateTime now) {
        Versamento versamento = new Versamento();
        if (versamentoDocInformation.getVersamentoPrecedente() != null) {
            Versamento versamentoPrecedente = entityManager.find(Versamento.class, versamentoDocInformation.getVersamentoPrecedente());
            versamento.setIdVersamentoRitentato(versamentoPrecedente);
        }
        versamento.setCodiceErrore(versamentoDocInformation.getCodiceErrore());
        versamento.setDescrizioneErrore(versamentoDocInformation.getDescrizioneErrore());
        versamento.setData(versamentoDocInformation.getDataVersamento());
        if (versamentoDocInformation.getTipologiaVersamento() == SessioneVersamento.TipologiaVersamento.FORZATURA)
            versamento.setDataForzatura(versamentoDocInformation.getDataVersamento());
        versamento.setIdDoc(doc);
        versamento.setIdArchivio(archivio);
        versamento.setIdPersonaForzatura(personaForzatura);
        versamento.setIdSessioneVersamento(sessioneVersamento);
        versamento.setMetadatiVersati(versamentoDocInformation.getMetadatiVersati());
        versamento.setRapporto(versamentoDocInformation.getRapporto());
        versamento.setStato(versamentoDocInformation.getStatoVersamento());
        versamento.setDataInserimento(now);

        return versamento;
    }
    
    private Versamento.StatoVersamento getStatoVersamentoDocFromStatoVersamentoAllegato(Versamento.StatoVersamento statoVersamentoDocAttuale, Versamento.StatoVersamento statoVersamentoAllegato) {
        if (statoVersamentoDocAttuale == null) {
            statoVersamentoDocAttuale = statoVersamentoAllegato;
        } else if (statoVersamentoAllegato == Versamento.StatoVersamento.ERRORE_RITENTABILE) {
            statoVersamentoDocAttuale = Versamento.StatoVersamento.ERRORE_RITENTABILE;
        } else if (statoVersamentoDocAttuale == Versamento.StatoVersamento.IN_CARICO) {
            if (statoVersamentoAllegato == Versamento.StatoVersamento.ERRORE) {
                statoVersamentoDocAttuale = Versamento.StatoVersamento.IN_CARICO_CON_ERRORI;
            }
        } else if (statoVersamentoAllegato == Versamento.StatoVersamento.ERRORE || statoVersamentoAllegato == Versamento.StatoVersamento.ERRORE_FORZABILE) {
            statoVersamentoDocAttuale = statoVersamentoAllegato;
        }
        
        return statoVersamentoDocAttuale;
    }
}
