package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CAMBIO_PADRE;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CHIUSURA;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CONFLUENZA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.UnificazioneDaGestire;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class OperationUnificazioneStruttura extends Operation<DatiRibaltoneInterface> implements Serializable {

    private StrutturaUnificata.TipoUnificazione tipoUnificazione;
    private List<UnificazioneDaGestire> unificazioniDaGestire;
    private Map<String, List<Struttura>> mappStrutturePerGestioneContatti = new HashMap();
    private OperationUnificazioneAppartenente.UnificazionePair.DirezioneReplica direzioneReplica;
    private List<UtenteStruttura> utentiStrutturaDaChiuderePerManageContatti = new ArrayList<>();

    private static final Logger log = LoggerFactory.getLogger(OperationAppartenente.class);

    public OperationUnificazioneStruttura(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager, StrutturaUnificata.TipoUnificazione tipoUnificazione, List<UnificazioneDaGestire> unificazioniDaGestire, Map<String, String> descrizioniAggiuntive, OperationUnificazioneAppartenente.UnificazionePair.DirezioneReplica direzioneReplica) {
        super(azione, entitaCoinvolta, entityManager, descrizioniAggiuntive);
        this.tipoUnificazione = tipoUnificazione;
        this.unificazioniDaGestire = unificazioniDaGestire;
        this.direzioneReplica = direzioneReplica;
    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(getEntityManager());
        QStruttura qStruttura = QStruttura.struttura;
        QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
        switch (getAzione()) {

            case INSERT -> {
                DatiDaImportareStruttura strutturaInserita = (DatiDaImportareStruttura) getEntitaCoinvolta();
                if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                    Struttura strutturaBaborgAperta = jPAQueryFactory
                            .select(qStruttura)
                            .from(qStruttura)
                            .where(qStruttura.idAzienda.id.eq(strutturaInserita.getIdAzienda())
                                    .and(qStruttura.idCasella.eq(strutturaInserita.getIdCasella())
                                            .and(qStruttura.attiva.eq(Boolean.TRUE)))
                            ).orderBy(qStruttura.id.desc()).fetchOne();
                    if (strutturaBaborgAperta != null) {
                        for (UnificazioneDaGestire unificazioneDaGestire : unificazioniDaGestire) {
                            StrutturaUnificata unificazione = getEntityManager().find(StrutturaUnificata.class, unificazioneDaGestire.getIdUnificazione());
                            if (unificazioneDaGestire.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                                Struttura cloneStrutturaForUnificazione = strutturaBaborgAperta.cloneStrutturaForUnificazione(unificazione);
                                cloneStrutturaForUnificazione.setIdAzienda(unificazione.getIdStrutturaDestinazione().getIdAzienda());
                                getEntityManager().persist(cloneStrutturaForUnificazione);
                                List<Struttura> struttureList = mappStrutturePerGestioneContatti.get(getAzione().toString());
                                if (struttureList == null) {
                                    struttureList = new ArrayList<>();
                                }
                                struttureList.add(cloneStrutturaForUnificazione);
                                mappStrutturePerGestioneContatti.put(getAzione().toString(), struttureList);
                                //ora cerco il padre per collegarlo
                                Struttura strutturaPadreDaCollegare = jPAQueryFactory
                                        .select(qStruttura)
                                        .from(qStruttura)
                                        .where(qStruttura.idAzienda.id.eq(unificazione.getIdStrutturaDestinazione().getIdAzienda().getId())
                                                .and(qStruttura.idStrutturaReplicata.idCasella.eq(strutturaInserita.getIdCasellaPadre()))
                                                .and(qStruttura.attiva.eq(Boolean.TRUE))
                                        ).orderBy(qStruttura.id.desc()).fetchOne();
                                if (strutturaPadreDaCollegare != null) {
                                    cloneStrutturaForUnificazione.setIdStrutturaPadre(strutturaPadreDaCollegare);

                                    //devo fare la parte di storico relazioni
                                    StoricoRelazione storicoRelazione = new StoricoRelazione();
                                    storicoRelazione.setAttivaDal(ZonedDateTime.now());
                                    storicoRelazione.setIdStrutturaPadre(strutturaPadreDaCollegare);
                                    storicoRelazione.setIdStrutturaFiglia(cloneStrutturaForUnificazione);

                                    getEntityManager().persist(storicoRelazione);
                                }

                                //ora cerco i miei figli per collegarli a me
                                List<Struttura> struttureFiglieNonAncoraCollegate = jPAQueryFactory
                                        .select(qStruttura)
                                        .from(qStruttura)
                                        .where(qStruttura.idAzienda.id.eq(unificazione.getIdStrutturaDestinazione().getIdAzienda().getId())
                                                .and(qStruttura.idStrutturaReplicata.idCasellaPadre.eq(strutturaInserita.getIdCasella()))
                                                .and(qStruttura.attiva.eq(Boolean.TRUE))
                                                .and(qStruttura.idStrutturaPadre.isNull())
                                        ).fetch();
                                if (struttureFiglieNonAncoraCollegate != null && !struttureFiglieNonAncoraCollegate.isEmpty()) {
                                    for (Struttura strutturaFigliaDaCollegare : struttureFiglieNonAncoraCollegate) {
                                        strutturaFigliaDaCollegare.setIdStrutturaPadre(cloneStrutturaForUnificazione);
                                        getEntityManager().persist(strutturaFigliaDaCollegare);
                                        //devo fare la parte di storico relazioni
                                        StoricoRelazione storicoRelazione = new StoricoRelazione();
                                        storicoRelazione.setAttivaDal(ZonedDateTime.now());
                                        storicoRelazione.setIdStrutturaPadre(cloneStrutturaForUnificazione);
                                        storicoRelazione.setIdStrutturaFiglia(strutturaFigliaDaCollegare);
                                        getEntityManager().persist(storicoRelazione);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            case CHIUSURA -> {
                DatiImportatiStruttura strutturaChiusa = (DatiImportatiStruttura) getEntitaCoinvolta();
                if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                    // se sono la sorgente della replica devo spegnere l'unificazione
                    // per ogni destinazione devo andare a cercare nell'azienda la struttura e chiuderla
                    // devo fare la stressa cosa per gli utenti struttura -- NO se ne occuperà il operationUnificazioneAppartentnte
                    // devo fare la stessa cosa per gli utenti che non hanno piu strutture sull'azienda
                    for (UnificazioneDaGestire unificazioneDaGestire : unificazioniDaGestire) {
                        if (unificazioneDaGestire.getIdCasellaSorgente().equals(strutturaChiusa.getIdCasella())
                                || unificazioneDaGestire.getIdCasellaDestinazione().equals(strutturaChiusa.getIdCasella())) {
                            //sono la replica principale quindi devo spegnere la replica,
                            StrutturaUnificata unificazioneDaSpegnere = getEntityManager().find(StrutturaUnificata.class, unificazioneDaGestire.getIdUnificazione());
                            unificazioneDaSpegnere.setDataDisattivazione(ZonedDateTime.now());
                            getEntityManager().persist(unificazioneDaSpegnere);
                        }

                        Struttura strutturaUnificataBaborgDaChiudere = jPAQueryFactory
                                .select(qStruttura)
                                .from(qStruttura)
                                .where(qStruttura.idAzienda.id.eq(unificazioneDaGestire.getIdAziendaDestinazione())
                                        .and(qStruttura.idStrutturaReplicata.idCasella.eq(strutturaChiusa.getIdCasella()))
                                        .and(qStruttura.attiva)
                                ).orderBy(qStruttura.id.desc()).limit(1).fetchOne();
                        OperationsUtils.chiudiStruttura(strutturaUnificataBaborgDaChiudere, jPAQueryFactory, qStruttura, qStoricoRelazione);
                        //todo da chiudere i permessi sulla struttura
                        OperationsUtils.chiudiPermessiStruttura(strutturaUnificataBaborgDaChiudere, repositoryFactory);
                        List<Struttura> struttureList = mappStrutturePerGestioneContatti.get(getAzione().toString());
                        if (struttureList == null) {
                            struttureList = new ArrayList<>();
                        }
                        struttureList.add(strutturaUnificataBaborgDaChiudere);
                        mappStrutturePerGestioneContatti.put(getAzione().toString(), struttureList);

                    }

                } else if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.FUSIONE)) {
                    for (UnificazioneDaGestire unificazioneDaGestire : unificazioniDaGestire) {
                        //probabilmente non devo fare nulla se non spegnere la fusione
                        //forse potrebbe essere gia spenta
                        if (unificazioneDaGestire.getIdCasellaSorgente().equals(strutturaChiusa.getIdCasella())
                                || unificazioneDaGestire.getIdCasellaDestinazione().equals(strutturaChiusa.getIdCasella())) {
                            StrutturaUnificata unificazioneDaSpegnere = getEntityManager().find(StrutturaUnificata.class, unificazioneDaGestire.getIdUnificazione());
                            if (unificazioneDaSpegnere != null && unificazioneDaSpegnere.getDataDisattivazione() == null) {
                                unificazioneDaSpegnere.setDataDisattivazione(ZonedDateTime.now());
                                getEntityManager().persist(unificazioneDaSpegnere);
                            }
                            //devo necessariamente spegnere tutti gli utenti struttura unificati qui

                            unificazioneDaSpegnere.getIdStrutturaSorgente().getUtenteStrutturaList().stream().forEach(
                                    us -> {
                                        if (us.getIdAfferenzaStruttura().getCodice().equals(AfferenzaStruttura.CodiciAfferenzaStruttura.UNIFICATA)) {
                                            OperationsUtils.chiudiUtenteStruttra(us, us.getIdUtente().getIdPersona(), repositoryFactory.getPermissionManager(), us.getIdUtente(), jPAQueryFactory, repositoryFactory.getEntityManager(), utentiStrutturaDaChiuderePerManageContatti);

                                        }
                                    });
                            unificazioneDaSpegnere.getIdStrutturaDestinazione().getUtenteStrutturaList().stream().forEach(
                                    us -> {
                                        if (us.getIdAfferenzaStruttura().getCodice().equals(AfferenzaStruttura.CodiciAfferenzaStruttura.UNIFICATA)) {
                                            OperationsUtils.chiudiUtenteStruttra(us, us.getIdUtente().getIdPersona(), repositoryFactory.getPermissionManager(), us.getIdUtente(), jPAQueryFactory, repositoryFactory.getEntityManager(), utentiStrutturaDaChiuderePerManageContatti);

                                        }
                                    });
                        }

                    }
                }
            }
            case CAMBIO_PADRE -> {
                /*
                 * Premesso che ho fatto le trasformaizoni delle unificaizoni,
                 * e quindi sulla tabella delle unifichazioni tutte le
                 * unificaizoni attive riguardano strutture attive
                 *
                 * NB: Escludiamo la gestione del caso in cui un trasferimento comporti
                 * l'inserimetno di una struttura già sorgente di replica come
                 * figlia di una struttura anch'essa sorgente di replica.
                 * Questo caso comporterebbe la doppia replica di una struttura.
                 * Essendo caso raro e di difficile gestione lo trascuriamo.
                 * Se accaddesse, dovrebbe essere trattabile a mano spegnendo una
                 * delle due unficazioni e lanciando uno sposta strutture
                 * tra la replica spenta e la replcia rimasta accesa
                 *
                 * CAMBIO_PADRE:
                 * - nel caso di fusioni devo solo aggiornare la fusione quindi
                 * non faccio nulla Lo sposta strutture delle unificazioni
                 * ha già sistemato la tabella unificaizoni
                 * - Nel caso di repliche:
                 * -- Se sono sorgente di replica non faccio nulla.
                 * Lo sposta strutture delle unificazioni ha già sistemato
                 * la tabella unificaizoni
                 * - Se non sorgente allora:
                 * -- Query1: Per cominciare spengo tutte le strutture accese che
                 * abbiano come id_struttura_replciata il mio
                 * id_struttura_vecchio, mi faccio tornare anche l'id e
                 * l'id_azienda della struttura spenta
                 * - Se come id_struttura_nuovo sono un discendente di replica allora
                 * mi replico dove devo:
                 * -- Se dalla Query1 ho l'id vecchio (dell'azienda corretta)
                 * allora faccio lo sposta strutture
                 */
                DatiDaImportareStruttura strutturaTrasferita = (DatiDaImportareStruttura) getEntitaCoinvolta();
                if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                    for (UnificazioneDaGestire unificazioneDaGestire : unificazioniDaGestire) {
                        StrutturaUnificata unificazione = getEntityManager().find(StrutturaUnificata.class, unificazioneDaGestire.getIdUnificazione());
                        if (unificazioneDaGestire.getIdCasellaSorgente().equals(strutturaTrasferita.getIdCasella())) {
                            //non devo fare nulla ad aggiornare la l'unificazione ci ha pensato gia lo sposta strutture
                        } else {
                            Struttura strutturaUnificataBaborgDaChiudere = jPAQueryFactory
                                    .select(qStruttura)
                                    .from(qStruttura)
                                    .where(qStruttura.idAzienda.id.eq(unificazioneDaGestire.getIdAziendaDestinazione())
                                            .and(qStruttura.idStrutturaReplicata.idCasella.eq(strutturaTrasferita.getIdCasella()))
                                            .and(qStruttura.idStrutturaReplicata.idAzienda.id.eq(strutturaTrasferita.getIdAzienda()))
                                            .and(qStruttura.attiva)
                                    ).orderBy(qStruttura.id.desc()).fetchOne();
                            if (strutturaUnificataBaborgDaChiudere != null) {
                                Struttura strutturaReplicaChiusa = OperationsUtils.chiudiStruttura(strutturaUnificataBaborgDaChiudere, jPAQueryFactory, qStruttura, qStoricoRelazione);
                                //todo chiudere i permessi struttura e veicolati
                                List<Struttura> struttureList = mappStrutturePerGestioneContatti.get(getAzione().toString());
                                if (struttureList == null) {
                                    struttureList = new ArrayList<>();
                                }
                                struttureList.add(strutturaUnificataBaborgDaChiudere);
                                mappStrutturePerGestioneContatti.put(getAzione().toString(), struttureList);
                                Struttura strutturaBaborgAperta = jPAQueryFactory
                                        .select(qStruttura)
                                        .from(qStruttura)
                                        .where(qStruttura.idAzienda.id.eq(strutturaTrasferita.getIdAzienda())
                                                .and(qStruttura.idCasella.eq(strutturaTrasferita.getIdCasella())
                                                        .and(qStruttura.attiva))
                                        ).orderBy(qStruttura.id.desc()).fetchOne();
                                if (strutturaBaborgAperta != null) {
                                    List<Struttura> padriSuCuiMiDevoReplicare = jPAQueryFactory
                                            .select(qStruttura)
                                            .from(qStruttura)
                                            .where(qStruttura.idStrutturaReplicata.id.eq(strutturaBaborgAperta.getIdStrutturaPadre().getId()))
                                            .fetch();
                                    for (Struttura strutturaPadre : padriSuCuiMiDevoReplicare) {
                                        Struttura strutturaReplicaNew = strutturaBaborgAperta.cloneStrutturaForUnificazione(unificazione);
                                        strutturaReplicaNew.setIdAzienda(strutturaPadre.getIdAzienda());
                                        strutturaReplicaNew.setIdStrutturaPadre(strutturaPadre);

                                        StoricoRelazione storicoRelazione = new StoricoRelazione();
                                        storicoRelazione.setAttivaDal(ZonedDateTime.now());
                                        storicoRelazione.setIdStrutturaPadre(strutturaPadre);
                                        storicoRelazione.setIdStrutturaFiglia(strutturaReplicaNew);
                                        getEntityManager().persist(storicoRelazione);
                                        OperationsUtils.inserisciSpostaUtentiStruttura(jPAQueryFactory, getEntityManager(), strutturaReplicaNew, strutturaReplicaChiusa);
                                        if (strutturaPadre.getIdAzienda().getId().equals(strutturaReplicaChiusa.getIdAzienda().getId())) {
                                            OperationsUtils.spostaStruttura(
                                                    getEntityManager(),
                                                    strutturaReplicaChiusa.getId(),
                                                    strutturaReplicaNew.getId(),
                                                    "T",
                                                    ZonedDateTime.now().toString());
                                        }
                                    }
                                }

                            }

                        }
                    }
                } else if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.FUSIONE)) {
                    //non devo fare nulla ad aggiornare la l'unificazione ci ha pensato gia lo sposta strutture
                }

            }

            case RINOMINA -> {
                //sono nel caso della rinomina in ogni caso potrei avere
//                //un trascorso e potrei essere sia sorgente e che destinazione di una FUSIONE

//                // oppure sono nel caso di REPLICA
                // e devo gestire il caso in cui una struttura replicata sia stata rinominata
                // - devo trovare la struttura da rinominare
                //   - sto cercando una struttura attiva che abbia come idStrutturaReplicata una struttura che ha id_casella =  entitaRinominata.id_casella
                //   - da li ho idStrutturaUnificata nuovo (se è stato cambiato con lo sposta strutture della struttura di partenza, caso in cui ho rinominato la sorgente)
                // - devo spegnere la struttura da rinominare
                // - devo creare la nuova struttura rinominata
                // - devo spostare gli utenti struttura sulla nuova
                // - devo chiamare lo sposta strutture
                // VECCHIO COMMENTO -----------------------------------------------
//                      il mio vecchio id sara dentro a idstruttura replicata quindi devo trovare il mio vecchio me
//                      altrimenti il mio vecchio id sara dentro idStrutturaReplicata
//                      andare a cercare la vecchia replica
//                      andare a creare la nuova struttura replica
//                      chiamare lo sposta struttura tra vecchio id e quello appena creato
//                      primo passo sono replica o sono fusione? o non sono nulla?
//                      se sono replica o figlio di replica allora trovero che io
//                      o uno dei miei antenati sta in un qualche idStrutturaReplicata
//                      oppure ce l'ho valorizzato se qualcuno è replicato in me
                DatiDaImportareStruttura strutturaRinominata = (DatiDaImportareStruttura) getEntitaCoinvolta();
                if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                    //devo rinominare anche dall'altro lato
                    //devo avere la struttura baborg rinominata

                    //adesso devo cercare la struttura baborg nell'azienda della replica
                    for (UnificazioneDaGestire unificazioneDaGestire : unificazioniDaGestire) {
                        StrutturaUnificata unificazione = getEntityManager().find(StrutturaUnificata.class, unificazioneDaGestire.getIdUnificazione());
                        Struttura strutturaBaborgReplicaDaChiudere = jPAQueryFactory
                                .select(qStruttura)
                                .from(qStruttura)
                                .where(qStruttura.idAzienda.id.eq(unificazioneDaGestire.getIdAziendaDestinazione())
                                        .and(qStruttura.idStrutturaReplicata.idCasella.eq(strutturaRinominata.getIdCasella())
                                                .and(qStruttura.attiva.eq(Boolean.TRUE)))
                                ).orderBy(qStruttura.id.desc()).fetchOne();
                        if (strutturaBaborgReplicaDaChiudere != null) {
                            Struttura idStrutturaReplicata = jPAQueryFactory
                                    .select(qStruttura)
                                    .from(qStruttura)
                                    .where(qStruttura.idAzienda.id.eq(unificazioneDaGestire.getIdAziendaSorgente())
                                            .and(qStruttura.idCasella.eq(strutturaRinominata.getIdCasella())
                                                    .and(qStruttura.attiva.eq(Boolean.TRUE)))
                                    ).orderBy(qStruttura.id.desc()).fetchOne();

                            Struttura strutturaNew = strutturaBaborgReplicaDaChiudere.cloneStrutturaForUnificazione(unificazione);
                            strutturaNew.setNome(strutturaRinominata.getDescrizione());
                            strutturaNew.setIdStrutturaPadre(strutturaBaborgReplicaDaChiudere.getIdStrutturaPadre());
                            strutturaNew.setIdAzienda(strutturaBaborgReplicaDaChiudere.getIdAzienda());
                            strutturaNew.setIdStrutturaReplicata(idStrutturaReplicata);

                            StoricoRelazione storicoRelazioneNew = new StoricoRelazione();
                            storicoRelazioneNew.setAttivaDal(ZonedDateTime.now());
                            storicoRelazioneNew.setIdStrutturaPadre(strutturaBaborgReplicaDaChiudere.getIdStrutturaPadre());
                            storicoRelazioneNew.setIdStrutturaFiglia(strutturaNew);
                            getEntityManager().persist(storicoRelazioneNew);
                            getEntityManager().flush();
                            getEntityManager().refresh(storicoRelazioneNew);
                            getEntityManager().persist(strutturaNew);
                            getEntityManager().flush();
                            getEntityManager().refresh(strutturaNew);

                            List<Struttura> struttureList = mappStrutturePerGestioneContatti.get(getAzione().toString());
                            if (struttureList == null) {
                                struttureList = new ArrayList<>();
                            }
                            struttureList.add(strutturaBaborgReplicaDaChiudere);
                            mappStrutturePerGestioneContatti.put(getAzione().toString(), struttureList);

                            OperationsUtils.chiudiStruttura(strutturaBaborgReplicaDaChiudere, jPAQueryFactory, qStruttura, qStoricoRelazione);
                            OperationsUtils.inserisciSpostaUtentiStruttura(jPAQueryFactory, getEntityManager(), strutturaNew, strutturaBaborgReplicaDaChiudere);
                            OperationsUtils.spostaStruttura(getEntityManager(),
                                    strutturaBaborgReplicaDaChiudere.getId(),
                                    strutturaNew.getId(),
                                    "R",
                                    strutturaNew.getDataAttivazione().toString()
                            );

                        }
                    }

                } else if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.FUSIONE)) {
                    //non devo fare nulla se non aggiornare la l'unificazione ma ci pensa lo sposta strutture della struttura rinominata
                }

            }

            case CONFLUENZA -> {
                //questo dovrebbe essere come il caso della chiusura quando confluisco una struttura le sue fusioni cessano di esistere
                DatiImportatiStruttura strutturaConfluita = (DatiImportatiStruttura) getEntitaCoinvolta();
                if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                    // se sono la sorgente della replica devo spegnere l'unificazione
                    // per ogni destinazione devo andare a cercare nell'azienda la struttura e chiuderla
                    // devo fare la stressa cosa per gli utenti struttura -- NO se ne occuperà il operationUnificazioneAppartentnte
                    // devo fare la stessa cosa per gli utenti che non hanno piu strutture sull'azienda
                    for (UnificazioneDaGestire unificazioneDaGestire : unificazioniDaGestire) {
                        if (unificazioneDaGestire.getIdCasellaSorgente().equals(strutturaConfluita.getIdCasella())
                                || unificazioneDaGestire.getIdCasellaDestinazione().equals(strutturaConfluita.getIdCasella())) {
                            //sono la replica principale quindi devo spegnere la replica,
                            StrutturaUnificata unificazioneDaSpegnere = getEntityManager().find(StrutturaUnificata.class, unificazioneDaGestire.getIdUnificazione());
                            unificazioneDaSpegnere.setDataDisattivazione(ZonedDateTime.now());
                            getEntityManager().persist(unificazioneDaSpegnere);
                        }
                        Struttura strutturaBaborgChiusa = jPAQueryFactory
                                .select(qStruttura)
                                .from(qStruttura)
                                .where(qStruttura.idAzienda.id.eq(strutturaConfluita.getIdAzienda())
                                        .and(qStruttura.idCasella.eq(strutturaConfluita.getIdCasella())
                                                .and(qStruttura.attiva.eq(Boolean.FALSE)))
                                ).orderBy(qStruttura.id.desc()).fetchOne();
                        if (strutturaBaborgChiusa != null) {
                            Integer idStrutturaBaborgSorgenteChiusa = strutturaBaborgChiusa.getId();

                            Struttura strutturaUnificataBaborgDaChiudere = jPAQueryFactory
                                    .select(qStruttura)
                                    .from(qStruttura)
                                    .where(qStruttura.idAzienda.id.eq(unificazioneDaGestire.getIdAziendaDestinazione())
                                            .and(qStruttura.idStrutturaReplicata.id.eq(idStrutturaBaborgSorgenteChiusa))
                                    ).orderBy(qStruttura.id.desc()).fetchOne();
                            OperationsUtils.chiudiStruttura(strutturaUnificataBaborgDaChiudere, jPAQueryFactory, qStruttura, qStoricoRelazione);
                            List<Struttura> struttureList = mappStrutturePerGestioneContatti.get(getAzione().toString());
                            if (struttureList == null) {
                                struttureList = new ArrayList<>();
                            }
                            struttureList.add(strutturaUnificataBaborgDaChiudere);

                        } else {
                            // nulla da chiudere?
                        }
                    }

                } else if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.FUSIONE)) {
                    for (UnificazioneDaGestire unificazioneDaGestire : unificazioniDaGestire) {
                        //probabilmente non devo fare nulla se non spegnere la fusione
                        //forse potrebbe essere gia spenta
                        if (unificazioneDaGestire.getIdCasellaSorgente().equals(strutturaConfluita.getIdCasella())
                                || unificazioneDaGestire.getIdCasellaDestinazione().equals(strutturaConfluita.getIdCasella())) {
                            StrutturaUnificata unificazioneDaSpegnere = getEntityManager().find(StrutturaUnificata.class, unificazioneDaGestire.getIdUnificazione());
                            if (unificazioneDaSpegnere != null && unificazioneDaSpegnere.getDataDisattivazione() != null) {
                                unificazioneDaSpegnere.setDataDisattivazione(ZonedDateTime.now());
                                getEntityManager().persist(unificazioneDaSpegnere);
                            }
                        }
                    }
                }

            }
            default ->
                throw new AssertionError();
        }
    }

    public void menageContattoStutturaUnificata(RepositoryFactory repositoryFactory) {
        repositoryFactory.getEntityManager();
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(getEntityManager());
        QPersona qPersona = QPersona.persona;
        //forse sposta struttura ci pensa gia alla  gestione dei contatti cambiati di padre o rinominati o chiusi o confluiti
        Persona ribaltonePersona = jPAQueryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq("RIBALTONE")).fetchFirst();
        for (String key : mappStrutturePerGestioneContatti.keySet()) {
            switch (Azione.valueOf(key)) {
                case INSERT -> {
                    for (Struttura str : mappStrutturePerGestioneContatti.get(key)) {
                        Contatto buildContattoAndDettaglio = str.buildContattoAndDettaglio(ribaltonePersona.getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(str.getIdAzienda().getId())).toList().get(0), ribaltonePersona, null);
                        str.setIdContatto(buildContattoAndDettaglio);
                        getEntityManager().persist(str);
                        getEntityManager().persist(buildContattoAndDettaglio);
                    }
                    getEntityManager().flush();
                }
                case CAMBIO_PADRE -> {

                }
                case RINOMINA -> {
//                    for (Struttura str : mappStrutturePerGestioneContatti.get(key)) {
//                        Contatto idContatto = str.getIdContatto();
//                        String descrizioneContattoStruttura = str.getNome() + " [ " + str.getIdAzienda().getNome();
//                        if (str.getIdCasella() != null) {
//                            descrizioneContattoStruttura = descrizioneContattoStruttura + " - " + str.getIdCasella().toString() + "]";
//                        } else {
//                            descrizioneContattoStruttura = descrizioneContattoStruttura + " ]";
//                        }
//                        idContatto.setNome(descrizioneContattoStruttura);
//                        for (DettaglioContatto dettaglioContatto : idContatto.getDettaglioContattoList()) {
//                            dettaglioContatto.setDescrizione(descrizioneContattoStruttura);
//                            getEntityManager().persist(dettaglioContatto);
//                        }
//                        getEntityManager().persist(str);
//                    }
                }
                case CONFLUENZA -> {
                }
                case CHIUSURA -> {

//                    for (Struttura str : mappStrutturePerGestioneContatti.get(key)) {
//                        Contatto idContatto = str.getIdContatto();
//                        idContatto.setEliminato(Boolean.TRUE);
//                        idContatto.setEliminatoDa("ribaltone");
//                        for (DettaglioContatto dettaglioContatto : idContatto.getDettaglioContattoList()) {
//                            dettaglioContatto.setEliminato(true);
//
//                            getEntityManager().persist(dettaglioContatto);
//                        }
//                        getEntityManager().persist(str);
//                    }
                }
                default ->
                    throw new AssertionError();
            }
        }
        //spengo i contatti di utenti unificati che ho dovuto spegnere perche fusi e la fusione non c'è piu
        OperationsUtils.gestisciContatti(repositoryFactory, null, utentiStrutturaDaChiuderePerManageContatti);
    }

    public StrutturaUnificata.TipoUnificazione getTipoUnificazione() {
        return tipoUnificazione;
    }

    public void setTipoUnificazione(StrutturaUnificata.TipoUnificazione tipoUnificazione) {
        this.tipoUnificazione = tipoUnificazione;
    }

    public List<UnificazioneDaGestire> getUnificazioniDaGestire() {
        return unificazioniDaGestire;
    }

    public void setUnificazioniDaGestire(List<UnificazioneDaGestire> unificazioniDaGestire) {
        this.unificazioniDaGestire = unificazioniDaGestire;
    }

    @Override
    public DatiRibaltoneInterface.TipologiaCsv getTipo() {
        return DatiRibaltoneInterface.TipologiaCsv.UNIFICAZIONI_STRUTTURE;
    }

    public Map<String, List<Struttura>> getMappStrutturePerGestioneContatti() {
        return mappStrutturePerGestioneContatti;
    }

    public void setMappStrutturePerGestioneContatti(Map<String, List<Struttura>> mappStrutturePerGestioneContatti) {
        this.mappStrutturePerGestioneContatti = mappStrutturePerGestioneContatti;
    }

    public OperationUnificazioneAppartenente.UnificazionePair.DirezioneReplica getDirezioneReplica() {
        return direzioneReplica;
    }

    public void setDirezioneReplica(OperationUnificazioneAppartenente.UnificazionePair.DirezioneReplica direzioneReplica) {
        this.direzioneReplica = direzioneReplica;
    }

}
