package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.RINOMINA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class OperationsUtils {

    private static final Logger log = LoggerFactory.getLogger(OperationsUtils.class);

    private final static QPersona qPersona = QPersona.persona;
    private final static QUtente qUtente = QUtente.utente;
    private final static QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;
    private final static QStruttura qStruttura = QStruttura.struttura;
    private final static QAfferenzaStruttura qffAfferenzaStruttura = QAfferenzaStruttura.afferenzaStruttura;

    public static enum KeyMapReplica {
        ID_CASELLA,
        ANCESTOR_LIST,
        HAS_REPLICA,
        REPLICHE
    }

    /**
     *
     * @param entityManager
     * @param queryFactory
     * @param idAzienda
     * @param idCasella
     * @param descrizione
     * @param idCasellaPadre
     * @param qStruttura
     * @param struttureDaAggiornareConPadre
     * @return
     */
    public static Struttura inserisciStruttura(
        EntityManager entityManager,
        JPAQueryFactory queryFactory,
        Integer idAzienda,
        Integer idCasella,
        String descrizione,
        Integer idCasellaPadre,
        QStruttura qStruttura,
        HashMap<Integer, List<Integer>> struttureDaAggiornareConPadre
    ) {
        Azienda azienda = entityManager.find(Azienda.class, idAzienda);
        if (idAzienda != null) {
            Struttura strutturaDaInserire = new Struttura(
                idCasella,
                descrizione,
                ZonedDateTime.now(),
                null,
                true,
                null,
                idCasella,
                idCasellaPadre,
                Boolean.FALSE,
                null,
                azienda,
                false
            );
            log.info("idCasellaPadre" + idCasellaPadre);
            log.info("idAzienda" + idAzienda);
            //da trovare il padre se non c'è devo segnarmela e poi sistemarla a meno che non sia la radice
            if (idCasellaPadre != null && idCasellaPadre != 0) {
                Struttura idStrutturaPadre = queryFactory
                    .select(qStruttura)
                    .from(qStruttura)
                    .where(qStruttura.attiva
                        .and(qStruttura.idCasella.eq(idCasellaPadre))
                        .and(qStruttura.idAzienda.id.eq(idAzienda))
                    )
                    .fetchOne();

                //Inserire su baborg strutture
                //Inserire su baborg storico relazione
                if (idStrutturaPadre != null) {
                    strutturaDaInserire.setIdStrutturaPadre(idStrutturaPadre);
                    entityManager.persist(strutturaDaInserire);
                    entityManager.flush();
                    StoricoRelazione storicoRelazione = new StoricoRelazione();
                    storicoRelazione.setAttivaDal(ZonedDateTime.now());
                    storicoRelazione.setIdStrutturaPadre(idStrutturaPadre);
                    storicoRelazione.setIdStrutturaFiglia(strutturaDaInserire);
                    entityManager.persist(storicoRelazione);
                    entityManager.flush();
                } else {
                    entityManager.persist(strutturaDaInserire);
                    entityManager.flush();
                    struttureDaAggiornareConPadre = putInMap(struttureDaAggiornareConPadre, strutturaDaInserire, idCasellaPadre);
                }
                //ho inserito una struttura quindi cerco se devo collegare qualcosa
                if (struttureDaAggiornareConPadre.containsKey(strutturaDaInserire.getIdCasella())) {
                    queryFactory
                        .update(qStruttura)
                        .set(qStruttura.idStrutturaPadre, strutturaDaInserire)
                        .where(qStruttura.id.in(struttureDaAggiornareConPadre.get(strutturaDaInserire.getIdCasella()))).execute();
                    for (Integer idStrutturaFiglia : struttureDaAggiornareConPadre.get(strutturaDaInserire.getIdCasella())) {
                        Struttura strutturaFiglia = entityManager.find(Struttura.class, idStrutturaFiglia);
                        if (strutturaFiglia != null) {
                            StoricoRelazione sr = new StoricoRelazione();
                            sr.setAttivaDal(ZonedDateTime.now());
                            sr.setIdStrutturaFiglia(strutturaFiglia);
                            sr.setIdStrutturaPadre(strutturaDaInserire);
                            entityManager.persist(sr);
                            entityManager.flush();
                        } else {
                            throw new RibaltoneHttpException("struttura figlia con id_casella_padre" + strutturaDaInserire.getIdCasella() + " non trovata questo non puo accadere");
                        }

                    }

                    struttureDaAggiornareConPadre.remove(strutturaDaInserire.getIdCasella());
                }
            } else {
                //sono la radice
                StoricoRelazione sr = new StoricoRelazione();
                sr.setAttivaDal(ZonedDateTime.now());
                sr.setIdStrutturaFiglia(strutturaDaInserire);
                entityManager.persist(sr);
                entityManager.flush();
                entityManager.persist(strutturaDaInserire);
                entityManager.flush();
            }

            entityManager.refresh(strutturaDaInserire);
            return strutturaDaInserire;
        }
        return null;
    }

    /**
     * funzione che inserise in mappa l'id della struttura con chiave id padre
     * se struttura non è null;
     *
     * @param mappa
     * @param struttura
     * @param idPadre
     * @return mappa
     */
    private static HashMap<Integer, List<Integer>> putInMap(HashMap<Integer, List<Integer>> mappa, Struttura struttura, Integer idCasellaPadre) {
        if (struttura != null) {
            if (mappa == null) {
                mappa = new HashMap<>();
                List<Integer> arrayList = new ArrayList<>();
                arrayList.add(struttura.getId());
                mappa.put(idCasellaPadre, arrayList);
            } else {
                List<Integer> listaDiFigliDiIdPadre;
                if (mappa.containsKey(idCasellaPadre)) {
                    listaDiFigliDiIdPadre = mappa.get(idCasellaPadre);
                    listaDiFigliDiIdPadre.add(struttura.getId());

                } else {
                    listaDiFigliDiIdPadre = new ArrayList<>();
                    listaDiFigliDiIdPadre.add(struttura.getId());

                }
                mappa.put(idCasellaPadre, listaDiFigliDiIdPadre);
            }
        }
        return mappa;
    }

    /**
     *
     * @param strutturaBaborgDaChiudere
     * @param queryFactory
     * @param qStruttura
     * @param qStoricoRelazione
     * @return struttura appena chiusa
     * @throws RibaltoneHttpException
     */
    public static Struttura chiudiStruttura(Struttura strutturaBaborgDaChiudere, JPAQueryFactory queryFactory, QStruttura qStruttura, QStoricoRelazione qStoricoRelazione) throws RibaltoneHttpException {

        //chiudere su baborg strutture
        //chiudere su baborg storico relazione
        //chiudere su baborg strutture unificate
        if (strutturaBaborgDaChiudere == null) {
            throw new RibaltoneHttpException("StrutturaSorgenteDaChiudere non fornita");
        } else {
            //chiudere su baborg strutture

            queryFactory
                .update(qStruttura)
                .set(qStruttura.attiva, false)
                .set(qStruttura.dataCessazione, ZonedDateTime.now())
                .setNull(qStruttura.codice)
                .where(qStruttura.id.eq(strutturaBaborgDaChiudere.getId())).execute();

            //chiudere su baborg storico relazione
            queryFactory
                .update(qStoricoRelazione)
                .set(qStoricoRelazione.attivaAl, ZonedDateTime.now())
                .where((qStoricoRelazione.attivaAl.isNull().or(qStoricoRelazione.attivaAl.after(ZonedDateTime.now()))).and(
                    qStoricoRelazione.idStrutturaFiglia.id.eq(strutturaBaborgDaChiudere.getId())
                        .or(qStoricoRelazione.idStrutturaPadre.id.eq(strutturaBaborgDaChiudere.getId())))
                ).execute();

            return queryFactory.select(qStruttura).from(qStruttura).where(qStruttura.id.eq(strutturaBaborgDaChiudere.getId())).fetchOne();
        }
    }

    public static void aggiustaUnificazioni(Integer idCasellaDaChiudere, Struttura strutturaAppenaInserita, EntityManager em, JPAQueryFactory queryFactory, QStrutturaUnificata qStrutturaUnificata) {
        List<StrutturaUnificata> sorgentiDaModificare = queryFactory.select(qStrutturaUnificata)
            .from(qStrutturaUnificata)
            .where(qStrutturaUnificata.idStrutturaSorgente.idCasella.eq(idCasellaDaChiudere)
                .and(qStrutturaUnificata.dataDisattivazione.isNull())).fetch();

        List<StrutturaUnificata> destinazioniDaModificare = queryFactory.select(qStrutturaUnificata)
            .from(qStrutturaUnificata)
            .where(qStrutturaUnificata.idStrutturaDestinazione.idCasella.eq(idCasellaDaChiudere)
                .and(qStrutturaUnificata.dataDisattivazione.isNull())).fetch();

        for (StrutturaUnificata sorgenteDaModificare : sorgentiDaModificare) {
            sorgenteDaModificare.setDataDisattivazione(ZonedDateTime.now());
            em.persist(sorgenteDaModificare);
            StrutturaUnificata strutturaUnificata = new StrutturaUnificata();
            strutturaUnificata.setIdStrutturaSorgente(strutturaAppenaInserita);
            strutturaUnificata.setDataAttivazione(ZonedDateTime.now());
            strutturaUnificata.setTipoOperazione(sorgenteDaModificare.getTipoOperazione());
            strutturaUnificata.setDataAccensioneAttivazione(ZonedDateTime.now());
            em.persist(strutturaUnificata);
        }

        for (StrutturaUnificata destinazioneDaModificare : destinazioniDaModificare) {
            destinazioneDaModificare.setDataDisattivazione(ZonedDateTime.now());
            em.persist(destinazioneDaModificare);
            StrutturaUnificata strutturaUnificata = new StrutturaUnificata();
            strutturaUnificata.setIdStrutturaDestinazione(strutturaAppenaInserita);
            strutturaUnificata.setDataAttivazione(ZonedDateTime.now());
            strutturaUnificata.setTipoOperazione(destinazioneDaModificare.getTipoOperazione());
            strutturaUnificata.setDataAccensioneAttivazione(ZonedDateTime.now());
            em.persist(strutturaUnificata);
        }
    }

    public static void spostaStruttura(EntityManager em, Integer idStrutturaSorgente, Integer idStrutturaDestinazione, String tipoOperazione, String dataTrasformazioneStr) {
        em.createNativeQuery("SELECT ribaltone_utils.sposta_struttura(:id_struttura_vecchia, :id_struttura_nuova, :tipo_operazione, :id_strutturtext_data_trasformazionea_vecchia);")
            .setParameter("id_struttura_vecchia", idStrutturaSorgente)
            .setParameter("id_struttura_nuova", idStrutturaDestinazione)
            .setParameter("tipo_operazione", tipoOperazione)
            .setParameter("id_strutturtext_data_trasformazionea_vecchia", dataTrasformazioneStr)
            .getSingleResult();

    }

    /**
     * *
     *
     * @param queryFactory
     * @param idCasella
     * @param idAzienda
     * @param qStruttura
     * @return
     */
    public static Struttura getStrutturaAttivaFromIdCasellaAndIdAzienda(JPAQueryFactory queryFactory, Integer idCasella, Integer idAzienda, QStruttura qStruttura) {
        return queryFactory
            .select(qStruttura)
            .from(qStruttura)
            .where(qStruttura.idCasella.eq(idCasella)
                .and(qStruttura.attiva)
                .and(qStruttura.idAzienda.id.eq(idAzienda)))
            .fetchFirst();
    }

    public static List<StrutturaUnificata> entitaCoinvoltaTouchUnificazioni(JPAQueryFactory queryFactory, DatiRibaltoneInterface entitaCoinvolta, QStruttura qStruttura) {
        Struttura strutturaCoinvolta = null;

        switch (entitaCoinvolta.getTipo()) {
            case APPARTENENTI:
                if (entitaCoinvolta.getClasse().equals(DatiDaImportareAppartenente.class.getCanonicalName())) {
                    DatiDaImportareAppartenente datiDaImportareAppartenente = (DatiDaImportareAppartenente) entitaCoinvolta;
                    strutturaCoinvolta = getStrutturaAttivaFromIdCasellaAndIdAzienda(queryFactory, datiDaImportareAppartenente.getIdCasella(), datiDaImportareAppartenente.getIdAzienda(), qStruttura);
                } else {
                    DatiImportatiAppartenente datiImportatiAppartenente = (DatiImportatiAppartenente) entitaCoinvolta;
                    strutturaCoinvolta = getStrutturaAttivaFromIdCasellaAndIdAzienda(queryFactory, datiImportatiAppartenente.getIdCasella(), datiImportatiAppartenente.getIdAzienda(), qStruttura);
                }
                break;
            case STRUTTURE:
                if (entitaCoinvolta.getClasse().equals(DatiDaImportareStruttura.class.getCanonicalName())) {
                    DatiDaImportareStruttura datiDaImportareStruttura = (DatiDaImportareStruttura) entitaCoinvolta;
                    strutturaCoinvolta = getStrutturaAttivaFromIdCasellaAndIdAzienda(queryFactory, datiDaImportareStruttura.getIdCasella(), datiDaImportareStruttura.getIdAzienda(), qStruttura);
                } else {
                    DatiImportatiStruttura datiImportatiStruttura = (DatiImportatiStruttura) entitaCoinvolta;
                    strutturaCoinvolta = getStrutturaAttivaFromIdCasellaAndIdAzienda(queryFactory, datiImportatiStruttura.getIdCasella(), datiImportatiStruttura.getIdAzienda(), qStruttura);
                }
                break;

            default:
                throw new AssertionError();
        }
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
        if (strutturaCoinvolta != null) {
            List<StrutturaUnificata> struttureUnificateAttive = queryFactory
                .select(qStrutturaUnificata)
                .from(qStrutturaUnificata)
                .where((qStrutturaUnificata.idStrutturaSorgente.id.eq(strutturaCoinvolta.getId())
                    .or(qStrutturaUnificata.idStrutturaDestinazione.id.eq(strutturaCoinvolta.getId())))
                    .and(qStrutturaUnificata.dataAttivazione.isNotNull()
                        .and(qStrutturaUnificata.dataAttivazione.before(ZonedDateTime.now()))
                        .and(
                            qStrutturaUnificata.dataDisattivazione.isNull()
                                .or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now()))
                        )
                    )
                )
                .fetch();

            return struttureUnificateAttive;
        } else {
            return null;
        }
    }

    /**
     * funzione che si occupa di gestire le unificazioni di strutture
     *
     * @param entityManager
     * @param listOfOperationStruttura
     */
//    public static void manageUnificazioni(EntityManager entityManager, List<OperationStruttura> listOfOperationStruttura) {
//        for (OperationStruttura operationStruttura : listOfOperationStruttura) {
//            //prendo tutti i padri della struttura
//            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
//            List<Struttura> antenatiSorgentiDiReplicazione = new ArrayList<>();
//            QStruttura qStruttura = QStruttura.struttura;
//            switch (operationStruttura.getAzione()) {
//                case INSERT -> {
//                    DatiDaImportareStruttura entitaDaInserire = (DatiDaImportareStruttura) operationStruttura.getEntitaCoinvolta();
//                    //prendo i padri della struttura aperta
//                    sincronizzaStrutturaUnificazioneSeCoinvolta(entityManager, queryFactory, operationStruttura, antenatiSorgentiDiReplicazione);
//                }
//                case CHIUSURA -> {
//                    DatiImportatiStruttura entitaDaChiudere = (DatiImportatiStruttura) operationStruttura.getEntitaCoinvolta();
//                    //prendo i padri della struttura chiusa
//                    //se sono replicato da qualche parte mi devo spegnere anche li
//                    QStruttura strutturaSub = new QStruttura("strutturaSub");
//                    //faccio l'update per disattivare nel caso mi trovi
//                    Struttura strutturaDaSpegnere = queryFactory.select(qStruttura).from(qStruttura).where(
//                        qStruttura.attiva.and(
//                            qStruttura.idStrutturaReplicata.id.eq(
//                                //intanto trovo la mia replica che è la struttura che contiene me come
//                                JPAExpressions
//                                    .select(strutturaSub.id)
//                                    .from(strutturaSub)
//                                    .where(
//                                        strutturaSub.attiva.eq(Boolean.FALSE)
//                                            .and(strutturaSub.idCasella.eq(entitaDaChiudere.getIdCasella()))
//                                            .and(strutturaSub.idAzienda.id.eq(entitaDaChiudere.getIdAzienda()))).limit(1)
//                            )
//                        )
//                    ).fetchOne();
//                    strutturaDaSpegnere.setAttiva(false);
//                    strutturaDaSpegnere.setDataCessazione(ZonedDateTime.now());
//
//                    QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
//                    queryFactory
//                        .select(qStoricoRelazione)
//                        .from(qStoricoRelazione)
//                        .where();
//
//                }
//                case CAMBIO_PADRE, RINOMINA -> {
//                    sincronizzaStrutturaUnificazioneSeCoinvolta(entityManager, queryFactory, operationStruttura, antenatiSorgentiDiReplicazione);
//                }
//
//            }
//        }
//    }
    /**
     *
     * @param queryFactory
     * @param idCasella
     * @param attiva
     * @return lista ordinata per livello di antenati struttura (dalla radice
     *         Direzione Generale fino ad arrivare a idCasella passata)
     */
    public static List<Struttura> getStruttureAntenateAttiveONo(EntityManager entityManager, Integer idCasella, Boolean attive, Integer idAzienda) {
        return entityManager.createNativeQuery(
            "SELECT * FROM baborg.strutture_antenate_attive_o_no(:idCasella, :attive, :idAzienda)", Struttura.class)
            .setParameter("idCasella", idCasella)
            .setParameter("attive", attive)
            .setParameter("idAzienda", idAzienda)
            .getResultList();

    }

    /**
     * controllo se qualcuno degli antentati di @param entitaDaInserire fa parte
     * di una unificazione e nel caso esegue le operazioni di sincronizzazione
     *
     * @param entityManager
     * @param queryFactory
     * @param entitaDaInserire
     * @param antenatiSorgentiDiReplicazione
     */
//    private static void sincronizzaStrutturaUnificazioneSeCoinvolta(EntityManager entityManager, JPAQueryFactory queryFactory, OperationStruttura operationStruttura, List<Struttura> antenatiSorgentiDiReplicazione) {
//        QStruttura qStruttura = QStruttura.struttura;
//        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
//        DatiDaImportareStruttura entitaDaInserire = (DatiDaImportareStruttura) operationStruttura.getEntitaCoinvolta();
//        antenatiSorgentiDiReplicazione.addAll(getStruttureAntenateAttiveONo(entityManager, entitaDaInserire.getIdCasella(), true, entitaDaInserire.getIdAzienda()));
//        List<Integer> idsStrutture = antenatiSorgentiDiReplicazione.stream().map(struttura -> struttura.getId()).collect(Collectors.toList());
//        //controllo se uno dei padri è coinvolti in una unificazione
//        //deve essere attiva l'unificazione
//        //se l'azione e INSERT e
//        //se l'unificazione è una FUSIONE allora è impossibile fondersi (la fusione fonde due strutture non due alberature)
//        //se l'unificazione è una REPLICA allora devo vedere se uno dei miei antenati è una sorgente e nel caso replicare da li
//        //se l'azione è CAMBIO_PADRE o RINOMINA
//        // se l'unificazione è una fusione devo vedere se il vecchio me era facente parte di una fusione e nel caso risistemarmi
//        //se l'unificazione è una replica devo edere se il vecchio me era facente parte di una replica e nel caso rireplicarmi
//        switch (operationStruttura.getAzione()) {
//            case INSERT -> {
//                List<StrutturaUnificata> replicazioniCoinvolte = queryFactory
//                    .select(qStrutturaUnificata)
//                    .from(qStruttura)
//                    .join(qStrutturaUnificata).on(qStruttura.id.eq(qStrutturaUnificata.idStrutturaSorgente.id))
//                    .where(
//                        (qStrutturaUnificata.dataDisattivazione.isNull().or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
//                            .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.REPLICA))
//                            .and(qStrutturaUnificata.idStrutturaSorgente.id.in(idsStrutture)))
//                    .fetch();
//                Boolean replica = false;
//                for (StrutturaUnificata strutturaUnificata : replicazioniCoinvolte) {
//                    //per ogni struttura trovata vado a inserire la nuova struttrua se non c'è già
//                    //                        Struttura strDestinazioneDiReplicazione = strutturaUnificata.getIdStrutturaDestinazione();
//                    for (Struttura s : antenatiSorgentiDiReplicazione) {
//                        if (s.getId().equals(strutturaUnificata.getIdStrutturaSorgente().getId())) {
//                            //dal prossimo giro devo iniziare a replicare l'alberatura
//                            replica = true;
//                        } else if (replica) {
//                            //controllo se sono gia stato replicato
//                            Struttura strutturaGiaReplicata = queryFactory
//                                .select(qStruttura)
//                                .from(qStruttura)
//                                .where(
//                                    qStruttura.attiva.and(qStruttura.idStrutturaReplicata.id.eq(s.getId()))).fetchOne();
//                            if (strutturaGiaReplicata == null) {
//                                //devo replicare la struttura
//                                //prendo il padre che sara quello che ha idstrutturaReplicata di padre di s
//                                Struttura strutturaPadreDiStrutturaDaReplicare = queryFactory
//                                    .select(qStruttura)
//                                    .from(qStruttura)
//                                    .where(
//                                        qStruttura.attiva.and(qStruttura.idStrutturaReplicata.id.eq(s.getIdStrutturaPadre().getId()))).fetchOne();
//
//                                AttributiStruttura attributiStruttura = new AttributiStruttura();
//                                attributiStruttura.setIdTipologiaStruttura(s.getAttributiStruttura().getIdTipologiaStruttura());
//                                Struttura strReplicata = new Struttura(
//                                    s.getCodice(),
//                                    s.getNome(),
//                                    ZonedDateTime.now(),
//                                    null,
//                                    true,
//                                    strutturaPadreDiStrutturaDaReplicare,
//                                    s.getIdCasella(),
//                                    strutturaPadreDiStrutturaDaReplicare.getIdCasella(),
//                                    s.getUfficio(),
//                                    attributiStruttura,
//                                    strutturaPadreDiStrutturaDaReplicare.getIdAzienda(),
//                                    false);
//                                strReplicata.setIdStrutturaReplicata(s);
//                                StoricoRelazione storicoRelazione = new StoricoRelazione();
//                                storicoRelazione.setAttivaDal(ZonedDateTime.now());
//                                storicoRelazione.setIdStrutturaFiglia(strReplicata);
//                                storicoRelazione.setIdStrutturaPadre(strutturaPadreDiStrutturaDaReplicare);
//
//                                entityManager.persist(storicoRelazione);
//                            }
//                        }
//                    }
//                }
//            }
//            case RINOMINA -> {
//                //sono nel caso del cambio padre o della rinomina in ogni caso potrei avere
//                //un trascorso e potrei essere sia sorgente e che destinazione di una fusione
//                //sia replica che fusione
//                //se sono replicatore
//                //il mio vecchio id sara dentro a idstruttura replicata quindi devo trovare il mio vecchio me
//                //altrimenti il mio vecchio id sara dentro idStrutturaReplicata
//                //andare a cercare la vecchia replica
//                //andare a creare la nuova struttura replica
//                //chiamare lo sposta struttura tra vecchio id e quello appena creato
//                //primo passo sono replica o sono fusione? o non sono nulla?
//                //se sono replica o figlio di replica allora trovero che io
//                //o uno dei miei antenati sta in un qualche idStrutturaReplicata
//                //oppure ce l'ho valorizzato se qualcuno è replicato in me
//                List<Struttura> struttureReplicheCoinvolteNellaRinomina = queryFactory
//                    .select(qStruttura)
//                    .from(qStruttura)
//                    .where(
//                        (qStruttura.dataCessazione.isNull().or(qStruttura.dataCessazione.after(ZonedDateTime.now())))
//                            .and(qStruttura.attiva
//                                .and(qStruttura.idStrutturaReplicata.idCasella.eq(entitaDaInserire.getIdCasella()))))
//                    .fetch();
//                Struttura strutturaNuovaDaCollegare = queryFactory
//                    .select(qStruttura)
//                    .from(qStruttura)
//                    .where(qStruttura.attiva
//                        .and(qStruttura.idCasella.eq(entitaDaInserire.getIdCasella()))
//                        .and(qStruttura.idAzienda.codice.eq(entitaDaInserire.getCodiceAzienda()))
//                    ).fetchOne();
//                if (!struttureReplicheCoinvolteNellaRinomina.isEmpty()) {
//                    //vuol dire che nella tabella delle unificazioni o sono sorgente o lo è un mio antenato
//
//                    Struttura strutturaVecchiaDaScollegare = struttureReplicheCoinvolteNellaRinomina.get(0).getIdStrutturaReplicata();
//
//                    if (strutturaVecchiaDaScollegare != null) {
//                        List<Struttura> struttureAntenateAttiveONo = getStruttureAntenateAttiveONo(entityManager, entitaDaInserire.getIdCasella(), Boolean.TRUE, strutturaVecchiaDaScollegare.getIdAzienda().getId());
//
//                        List<StrutturaUnificata> replicheDaValutare = queryFactory
//                            .select(qStrutturaUnificata)
//                            .from(qStrutturaUnificata)
//                            .where(qStrutturaUnificata.dataAttivazione.before(ZonedDateTime.now())
//                                .and(
//                                    qStrutturaUnificata.dataDisattivazione.isNull()
//                                        .or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
//                                .and(qStrutturaUnificata.idStrutturaSorgente.in(struttureAntenateAttiveONo))
//                            ).fetch();
//                        boolean accendiNuovaUnificazione = false;
//                        for (StrutturaUnificata replica : replicheDaValutare) {
//                            if (replica.getIdStrutturaSorgente().getIdCasella().equals(entitaDaInserire.getIdCasella())) {
//                                //spegno questa e ne faccio una nuova per avere lo storico
//                                replica.setDataDisattivazione(ZonedDateTime.now());
//                                entityManager.persist(replica);
//                                accendiNuovaUnificazione = true;
//
//                            } else {
//                                //è un mio antenato a essere replicato quindi non devo fare nulla
//                            }
//                        }
//
//                        for (Struttura idStrutturaDestinazioneVecchia : struttureReplicheCoinvolteNellaRinomina) {
//
//                            idStrutturaDestinazioneVecchia.setAttiva(false);
//                            idStrutturaDestinazioneVecchia.setDataCessazione(ZonedDateTime.now());
//                            entityManager.persist(idStrutturaDestinazioneVecchia);
//
//                            Struttura idStrutturaDestinazioneNuova = new Struttura(
//                                idStrutturaDestinazioneVecchia.getCodice(),
//                                entitaDaInserire.getDescrizione(),
//                                ZonedDateTime.now(),
//                                null,
//                                Boolean.TRUE,
//                                idStrutturaDestinazioneVecchia.getIdStrutturaPadre(),
//                                idStrutturaDestinazioneVecchia.getIdCasella(),
//                                idStrutturaDestinazioneVecchia.getIdCasellaPadre(),
//                                idStrutturaDestinazioneVecchia.getUfficio(),
//                                idStrutturaDestinazioneVecchia.getAttributiStruttura(),
//                                idStrutturaDestinazioneVecchia.getIdAzienda(),
//                                idStrutturaDestinazioneVecchia.getSpettrale()
//                            );
//
//                            if (accendiNuovaUnificazione) {
//                                List<StrutturaUnificata> replicheCorrelate = replicheDaValutare.stream().filter(u -> Objects.equals(u.getIdStrutturaDestinazione().getId(), idStrutturaDestinazioneVecchia.getId())).toList();
//
//                                for (StrutturaUnificata strutturaUnificata : replicheCorrelate) {
//                                    StrutturaUnificata nuovaUnificazione = new StrutturaUnificata();
//                                    nuovaUnificazione.setIdStrutturaDestinazione(idStrutturaDestinazioneNuova);
//                                    nuovaUnificazione.setIdStrutturaSorgente(strutturaNuovaDaCollegare);
//                                    nuovaUnificazione.setTipoOperazione(strutturaUnificata.getTipoOperazione());
//                                    nuovaUnificazione.setDataAttivazione(ZonedDateTime.now());
//                                    nuovaUnificazione.setDataInserimentoRiga(ZonedDateTime.now());
//                                    nuovaUnificazione.setDataAccensioneAttivazione(ZonedDateTime.now());
//                                    entityManager.persist(nuovaUnificazione);
//                                }
//                            }
//
//                            entityManager.persist(idStrutturaDestinazioneNuova);
//                            //Gestisco lo storicoRelazione
//                            gestisciStoricoRelazione(queryFactory, entityManager, idStrutturaDestinazioneNuova, idStrutturaDestinazioneVecchia);
//                            //Gestisco gli UtentiStruttura
//                            attivaUtentiStruttura(queryFactory, entityManager, idStrutturaDestinazioneNuova, idStrutturaDestinazioneVecchia);
//                            //sposta struttura
//                            OperationsUtils.spostaStruttura(entityManager, idStrutturaDestinazioneVecchia.getId(), idStrutturaDestinazioneNuova.getId(), "R", idStrutturaDestinazioneNuova.getDataAttivazione().toString());
//                        }
//                    }
//                } else {
//                    //nessuno è replicato
//                }       //prendiamo in considerazione le fusioni non devo fare nulla perche
//                //ogni azienda ha nel proprio organigramma il nome di una struttura fusa
//                //se lo gestiscono da organigramma devo solo aggiornare la tabella StrutturaUnificata
//                gestioneFausione(entityManager, queryFactory, strutturaNuovaDaCollegare);
//            }
//            case CAMBIO_PADRE -> {
//                /*
//                 * Premesso che ho fatto le trasformaizoni delle unificaizoni, e quindi sulla tbaella delle unifichazioni tutte le unificaizoni attive riguardano strutture attive
//                 *
//                 * NB: Escludiamo la gestione del caso in cui un trasferimento comporti l'inserimetno di una struttura già sorgente di replica come figlia di una struttura anch'essa sorgente di replica.
//                 * Questo caso comporterebbe la doppia replica di una struttura. Essendo caso raro e di difficile gestione lo trascuriamo.
//                 * Se accaddesse, dovrebbe essere trattabile a mano spegnendo una delle due unficazioni e lanciando uno sposta strutture tra la replica spenta e la replcia rimasta accesa
//                 *
//                 * cambio padre:
//                 * - nel caso di fusioni devo solo aggiornare la fusione quindi non faccio nulla Lo sposta strutture delle unificazioni ha già sistemato la tabella unificaizoni
//                 * - nel caso di repliche:
//                 * -- Se sono sorgente di replica non faccio nulla. Lo sposta strutture delle unificazioni ha già sistemato la tabella unificaizoni
//                 * -- Se non sorgente allora:
//                 * - Query1: Per cominciare spengo tutte le strutture accese che abbiano come id_struttura_replciata il mio id_struttura_vecchio, mi faccio tornare anche l'id e l'id_azienda della struttura spenta
//                 * - Se come id_struttura_nuovo sono un discendente di replica allora mi replico dove devo:
//                 * -- Se dalla Query1 ho l'id vecchio (dell'azienda corretta) allora faccio lo sposta strutture
//                 */
//                Struttura strutturaNuovaDaCollegare =
//
//                if (strutturaNuovaDaCollegare != null) {
//                    gestioneaFusione(entityManager, queryFactory, strutturaNuovaDaCollegare);
//
//                    //inizio a gestire la replica
//                    List<Struttura> struttureAntenateAttive = getStruttureAntenateAttiveONo(
//                        entityManager,
//                        strutturaNuovaDaCollegare.getIdCasella(),
//                        Boolean.TRUE, strutturaNuovaDaCollegare.getIdAzienda().getId());
//
//                    List<StrutturaUnificata> unificazioneReplica = queryFactory
//                        .select(qStrutturaUnificata)
//                        .from(qStrutturaUnificata)
//                        .where(qStrutturaUnificata.idStrutturaSorgente.in(struttureAntenateAttive) < -controllare con id casella
//                        .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.REPLICA))
//                        .and(qStrutturaUnificata.dataAttivazione.before(ZonedDateTime.now()))
//                        .and(qStrutturaUnificata.dataDisattivazione.isNull().or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
//                    ).fetch();
//
//                    if (unificazioneReplica != null && !unificazioneReplica.isEmpty()) {
//                        StrutturaUnificata strutturaUnificata = new StrutturaUnificata();
//                        StrutturaUnificata strutturaUnificataOld = unificazioneReplica.get(0);
//                        if (unificazioneReplica.size() == 1 && unificazioneReplica.get(0).getIdStrutturaSorgente().getIdCasella().equals(strutturaNuovaDaCollegare.getIdCasella())) {
//                            //sono sorgente mio padre non ha una replica quindi mi cambio di padre e basta
//                            strutturaUnificata.setDataAccensioneAttivazione(ZonedDateTime.now());
//                            strutturaUnificata.setDataAttivazione(ZonedDateTime.now());
//                            strutturaUnificata.setDataInserimentoRiga(ZonedDateTime.now());
//                            strutturaUnificata.setIdStrutturaDestinazione(strutturaUnificata.getIdStrutturaDestinazione());
//                            strutturaUnificata.setIdStrutturaSorgente(strutturaNuovaDaCollegare);
//                            strutturaUnificata.setTipoOperazione(strutturaUnificataOld.getTipoOperazione());
//                            entityManager.persist(strutturaUnificata);
//
//                            strutturaUnificataOld.setDataDisattivazione(ZonedDateTime.now());
//                            entityManager.persist(strutturaUnificataOld);
//                        } else if (unificazioneReplica.size() > 1) {
//                            //caso in cui due strutture repliche distinte diventano figlie di una terza replica ignoro il tutto e vedo come si gestisce a mano
//                            //concordato con gus
//
//                            //un mio antenato è replica e io sono un discendente
//                            //posso anche essere anche diventato discendente di
//                            //una fusione pre esistente
//                        }
//                    }
//                } else {
//                    throw new RibaltoneHttpException("non è stata trovata la struttura con id_casella "
//                        + entitaDaInserire.getIdCasella().toString()
//                        + " attiva questo non puo accadere");
//                }
//            }
//            default -> {
//            }
//        }
//    }
    private static void gestisciStoricoRelazione(JPAQueryFactory queryFactory, EntityManager entityManager, Struttura idStrutturaDestinazioneNuova, Struttura idStrutturaDestinazioneVecchia) {
        QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;

        StoricoRelazione storicoRelazioneFiglioVecchio = queryFactory
            .select(qStoricoRelazione)
            .from(qStoricoRelazione)
            .where(qStoricoRelazione.attivaDal.before(ZonedDateTime.now())
                .and(qStoricoRelazione.attivaAl.isNull().or(qStoricoRelazione.attivaAl.after(ZonedDateTime.now())))
                .and(qStoricoRelazione.idStrutturaFiglia.id.eq(idStrutturaDestinazioneVecchia.getId()))
            ).fetchOne();

        if (storicoRelazioneFiglioVecchio != null) {
            storicoRelazioneFiglioVecchio.setAttivaAl(ZonedDateTime.now());
            entityManager.persist(storicoRelazioneFiglioVecchio);

            StoricoRelazione storicoRelazioneFiglioNuovo = new StoricoRelazione();
            storicoRelazioneFiglioNuovo.setAttivaDal(ZonedDateTime.now());
            storicoRelazioneFiglioNuovo.setIdStrutturaFiglia(idStrutturaDestinazioneNuova);
            storicoRelazioneFiglioNuovo.setIdStrutturaPadre(storicoRelazioneFiglioVecchio.getIdStrutturaPadre());
            entityManager.persist(storicoRelazioneFiglioNuovo);
        }

        List<StoricoRelazione> storicoRelazionePadriVecchi = queryFactory
            .select(qStoricoRelazione)
            .from(qStoricoRelazione)
            .where(qStoricoRelazione.attivaDal.before(ZonedDateTime.now())
                .and(qStoricoRelazione.attivaAl.isNull().or(qStoricoRelazione.attivaAl.after(ZonedDateTime.now())))
                .and(qStoricoRelazione.idStrutturaPadre.id.eq(idStrutturaDestinazioneVecchia.getId()))
            ).fetch();

        for (StoricoRelazione storicoRelazionePadreVecchio : storicoRelazionePadriVecchi) {
            if (storicoRelazionePadreVecchio != null) {
                storicoRelazionePadreVecchio.setAttivaAl(ZonedDateTime.now());
                entityManager.persist(storicoRelazionePadreVecchio);

                StoricoRelazione storicoRelazionePadreNuovo = new StoricoRelazione();
                storicoRelazionePadreNuovo.setAttivaDal(ZonedDateTime.now());
                storicoRelazionePadreNuovo.setIdStrutturaFiglia(storicoRelazionePadreVecchio.getIdStrutturaFiglia());
                storicoRelazionePadreNuovo.setIdStrutturaPadre(idStrutturaDestinazioneNuova);
                entityManager.persist(storicoRelazionePadreNuovo);
            }
        }
    }

    public static void inserisciSpostaUtentiStruttura(JPAQueryFactory queryFactory, EntityManager entityManager, Struttura idStrutturaNuova, Struttura idStrutturaVecchia) {
        QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;
        Azienda idAziendaNew = idStrutturaNuova.getIdAzienda();
        //prendo gli utenti della struttura vecchia
        List<UtenteStruttura> utentiStrutturaDaAggiornare = queryFactory
            .select(qUtenteStruttura)
            .from(qUtenteStruttura)
            .where(
                qUtenteStruttura.attivo.and(
                    qUtenteStruttura.idStruttura.id.eq(idStrutturaVecchia.getId())
                        .and(qUtenteStruttura.attivoAl.isNull().or(qUtenteStruttura.attivoAl.after(ZonedDateTime.now())))
                        .and(qUtenteStruttura.attivoDal.before(ZonedDateTime.now()))
                )
            ).fetch();
        //per ogniuno di loro creo sulla struttura nuova utenza se non c'è (caso di unificazione) e afferenza
        for (UtenteStruttura utenteStrutturaDaAggiornare : utentiStrutturaDaAggiornare) {
            Persona idPersona = utenteStrutturaDaAggiornare.getIdUtente().getIdPersona();
            List<Utente> utentiList = idPersona.getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(idStrutturaNuova.getIdAzienda().getId())).toList();
            Utente utenteNew;
            if (utentiList == null || utentiList.isEmpty()) {
                //bisogna creare l'utente dell'azienda
                utenteNew = new Utente();
                utenteNew.setAttivo(Boolean.TRUE);
                utenteNew.setIdAzienda(idAziendaNew);
                utenteNew.setIdPersona(idPersona);
                utenteNew.setUsername(idPersona.getCodiceFiscale());
                entityManager.persist(utenteNew);
            } else {
                utenteNew = utentiList.get(0);
                utenteNew.setAttivo(Boolean.TRUE);
                entityManager.persist(utenteNew);
            }

            UtenteStruttura utenteStrutturaNew = UtenteStruttura.clone(utenteStrutturaDaAggiornare);
            utenteStrutturaNew.setIdStruttura(idStrutturaNuova);
            utenteStrutturaNew.setAttivoDal(ZonedDateTime.now());
            utenteStrutturaNew.setAttivo(Boolean.TRUE);
            utenteStrutturaNew.setIdUtente(utenteNew);
            entityManager.persist(utenteStrutturaNew);

            if (idStrutturaNuova.getIdAzienda().getId().equals(idStrutturaVecchia.getIdAzienda().getId())) {
                //se entro qui vuol dire che sto usando questa funzione per fare uno sposta strutture interno all'azienda
                //altrimenti sto facendo una unificazione quindi devo "copiare" gli utenti struttura
                utenteStrutturaDaAggiornare.setAttivoAl(ZonedDateTime.now());
                utenteStrutturaDaAggiornare.setAttivo(Boolean.FALSE);
                utenteStrutturaDaAggiornare.setIdDettaglioContatto(null);
                entityManager.persist(utenteStrutturaDaAggiornare);
            }
        }
    }

    private static void gestioneFusione(EntityManager entityManager, JPAQueryFactory queryFactory, Struttura strutturaNuovaDaCollegare) {
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
        List<StrutturaUnificata> fusioniCoinvolte = queryFactory
            .select(qStrutturaUnificata)
            .from(qStrutturaUnificata)
            .where(
                (qStrutturaUnificata.dataDisattivazione.isNull().or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
                    .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.FUSIONE))
                    .and(qStrutturaUnificata.idStrutturaSorgente.idCasella.eq(strutturaNuovaDaCollegare.getIdCasella()).or(
                        qStrutturaUnificata.idStrutturaDestinazione.idCasella.eq(strutturaNuovaDaCollegare.getIdCasella()))))
            .fetch();
//
        if (!fusioniCoinvolte.isEmpty()) {
            // devo fare qualcosa
            for (StrutturaUnificata unificazioneFusione : fusioniCoinvolte) {
                StrutturaUnificata strutturaUnificataNew = new StrutturaUnificata();
                strutturaUnificataNew.setDataAccensioneAttivazione(ZonedDateTime.now());
                strutturaUnificataNew.setDataAttivazione(ZonedDateTime.now());
                strutturaUnificataNew.setDataInserimentoRiga(ZonedDateTime.now());
                strutturaUnificataNew.setTipoOperazione(StrutturaUnificata.TipoUnificazione.FUSIONE);
                strutturaUnificataNew.setIdStrutturaDestinazione(unificazioneFusione.getIdStrutturaDestinazione());
                strutturaUnificataNew.setIdStrutturaSorgente(unificazioneFusione.getIdStrutturaSorgente());
                if (unificazioneFusione.getIdStrutturaDestinazione().getIdCasella().equals(strutturaNuovaDaCollegare.getIdCasella())) {
                    strutturaUnificataNew.setIdStrutturaDestinazione(strutturaNuovaDaCollegare);
                } else if (unificazioneFusione.getIdStrutturaSorgente().getIdCasella().equals(strutturaNuovaDaCollegare.getIdCasella())) {
                    strutturaUnificataNew.setIdStrutturaSorgente(strutturaNuovaDaCollegare);
                }
                unificazioneFusione.setDataDisattivazione(ZonedDateTime.now());
                entityManager.persist(unificazioneFusione);
                entityManager.persist(strutturaUnificataNew);
            }

        } else {
            //basta ho finito per questa struttura
        }
    }

    public static void inserisciStrutturaNewInAziendaUnificata(JPAQueryFactory queryFactory, EntityManager em, Struttura strutturaNew, List<Struttura> struttureOld, Operation.Azione azione, StrutturaUnificata su) {
        for (Struttura strutturaOld : struttureOld) {
            if (!strutturaOld.getIdAzienda().getId().equals(strutturaNew.getIdAzienda().getId())) {
                Struttura strutturaNuovaPerAziendaUnificata = strutturaOld.cloneStrutturaForUnificazione(su);
                strutturaNuovaPerAziendaUnificata.setIdAzienda(strutturaOld.getIdAzienda());
                strutturaNuovaPerAziendaUnificata.setIdStrutturaReplicata(strutturaNew);
                Contatto buildContattoAndDettaglio = strutturaNuovaPerAziendaUnificata.buildContattoAndDettaglio(strutturaOld.getIdContatto().getIdUtenteCreazione(), strutturaOld.getIdContatto().getIdPersonaCreazione(), strutturaOld.getIdContatto().getIdAziende());
                strutturaNuovaPerAziendaUnificata.setIdContatto(buildContattoAndDettaglio);
                em.persist(strutturaNuovaPerAziendaUnificata);
                OperationsUtils.gestisciStoricoRelazione(queryFactory, em, strutturaNuovaPerAziendaUnificata, strutturaOld);
                OperationsUtils.inserisciSpostaUtentiStruttura(queryFactory, em, strutturaNuovaPerAziendaUnificata, strutturaOld);

                OperationsUtils.spostaStruttura(em, strutturaOld.getId(), strutturaNuovaPerAziendaUnificata.getId(), azione.equals(RINOMINA) ? "R" : "T", strutturaNuovaPerAziendaUnificata.getDataAttivazione().toString());
            }
        }
    }

    public static void insertUtenteInStruttura(JPAQueryFactory queryFactory, DatiDaImportareAppartenente entitaDaInserire, Struttura struttura, EntityManager entityManager, PermissionManager permissionManager, List<UtenteStruttura> utenteStrutturaDaInserireList) {
        insertUtenteInStruttura(queryFactory, entitaDaInserire, struttura, entityManager, permissionManager, utenteStrutturaDaInserireList, null);
    }

    public static void insertUtenteInStruttura(JPAQueryFactory queryFactory, DatiDaImportareAppartenente entitaDaInserire, Struttura struttura, EntityManager entityManager, PermissionManager permissionManager, List<UtenteStruttura> utenteStrutturaDaInserireList, Integer idFonteAggiunta) {
        Persona persona = getPersona(queryFactory, entitaDaInserire);

        //inserire in baborg persone se non c'è la persona
        if (struttura != null) {
            if (persona == null) {
                persona = new Persona();
                persona.setIdAziendaDefault(struttura.getIdAzienda());
                persona.setDescrizione(entitaDaInserire.getCognome() + " " + entitaDaInserire.getNome());
                persona.setNome(entitaDaInserire.getNome());
                persona.setCognome(entitaDaInserire.getCognome());
                persona.setCodiceFiscale(entitaDaInserire.getCodiceFiscale());
                persona.setIdSecondario(entitaDaInserire.getCodiceMatricola());
            }
            persona.setAttiva(Boolean.TRUE);
            persona.setIdSecondario(entitaDaInserire.getCodiceMatricola());
            //inserire in baborg utenti se non c'è l'utente dell'azienda che lancia il ribaltone
            //nel caso si stia trattando una struttura unificata allora controllo che si sia
            //e nel caso inserisco in quelle aziende l'utente nuovo
            Utente utente = null;
            if (persona.getId() != null) {
                utente = getUtenteDiIdAzienda(queryFactory, struttura.getIdAzienda().getId(), persona);
            }
            if (utente == null) {
                utente = new Utente();
                utente.setIdAzienda(struttura.getIdAzienda());
            }
            String username = entitaDaInserire.getUsername() != null ? entitaDaInserire.getUsername() : persona.getCodiceFiscale();
            utente.setUsername(username);
            utente.setOmonimia(Boolean.FALSE);
            utente.setAttivo(Boolean.TRUE);
            utente.setDataSpegnimento(null);
            utente.setIdPersona(persona);
            entityManager.persist(utente);
            entityManager.flush();
            UtenteStruttura utenteStruttura = getUtenteStrutturaAttivo(queryFactory, struttura, utente);
            if (utenteStruttura == null) {
                utenteStruttura = new UtenteStruttura();
                utenteStruttura.setIdUtente(utente);
                utenteStruttura.setAttivoDal(ZonedDateTime.now());
                utenteStruttura.setIdStruttura(struttura);
                utenteStruttura.setAttivo(Boolean.TRUE);
                //inserire in baborg utenti_struttura se non c'è l'afferenza ricordandosi di una sola afferenza diretta e n funzionali
                utenteStruttura.setIdAfferenzaStruttura(getAfferenzaFromSigla(queryFactory, entitaDaInserire.getIdAzienda().equals(struttura.getIdAzienda().getId()) ? entitaDaInserire.getTipoAppartenenza() : "U", utente));
                utenteStruttura.setResponsabile(entitaDaInserire.getResponsabile());
                utenteStruttura.setIdFonteAggiuntaAppartenente(idFonteAggiunta);
                log.info("sto creando l'utente struttura username: " + username + " su struttura: " + utenteStruttura.getIdStruttura().getIdCasella());
                entityManager.persist(utenteStruttura);
                if (utenteStrutturaDaInserireList != null) {
                    utenteStrutturaDaInserireList.add(utenteStruttura);
                }
            }
            log.info("sto gestendo username: " + username + " su struttura: " + utenteStruttura.getIdStruttura().getIdCasella());
            if (entitaDaInserire.getResponsabile()) {
                //inserire i permessi di flusso per i responsabili
                try {
                    permissionManager.insertSimplePermission(
                        utente,
                        struttura,
                        BlackBoxConstants.Predicato.FIRMA.toString(),
                        "ribaltone",
                        Boolean.FALSE,
                        Boolean.FALSE,
                        BlackBoxConstants.Ambito.PICO.toString(),
                        BlackBoxConstants.Tipo.FLUSSO.toString());
                    permissionManager.insertSimplePermission(
                        utente,
                        struttura,
                        BlackBoxConstants.Predicato.REDIGE.toString(),
                        "ribaltone",
                        Boolean.FALSE,
                        Boolean.FALSE,
                        BlackBoxConstants.Ambito.PICO.toString(),
                        BlackBoxConstants.Tipo.FLUSSO.toString());
                } catch (BlackBoxPermissionException ex) {
                    throw new RibaltoneHttpException("errore nella creazione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                }
            }

        } else {
            throw new RibaltoneHttpException(" non trovata la struttura di un utente qualcosa nei controlli è andato male!!!");
        }

    }

    public static Persona getPersona(JPAQueryFactory queryFactory, DatiDaImportareAppartenente entitaDaInserire) {
        //faccio fetchFirst perche mi aspetto di trovare un solo utente per azienda o di non trovarne affatto
        return queryFactory
            .select(qPersona)
            .from(qPersona)
            .where(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale()))
            .fetchFirst();
    }

    public static Utente getUtenteDiIdAzienda(JPAQueryFactory queryFactory, Integer idAzienda, Persona persona) {
        if (persona == null) {
            return null;
        } else {
            List<Utente> utenti = queryFactory
                .select(qUtente)
                .from(qUtente)
                .where(qUtente.idPersona.id.eq(persona.getId())
                    .and(qUtente.idAzienda.id.eq(idAzienda)))
                .fetch();
            if (utenti == null || utenti.isEmpty()) {
                return null;
            } else {
                return utenti.get(0);

            }
        }
    }

    public static UtenteStruttura getUtenteStrutturaAttivo(JPAQueryFactory queryFactory, Struttura struttura, Utente utente) {
        if (struttura != null && utente.getId() != null) {
            log.info("utente cf: " + utente.getIdPersona().getCodiceFiscale());
            log.info("utente id: " + utente.getId());
            log.info("struttura id_casella: " + struttura.getIdCasella());
            Utente userPerQuery = utente;
            if (!utente.getIdAzienda().getId().equals(struttura.getIdAzienda().getId())) {
                List<Utente> utentiDiAziendaDiStruttura = utente.getIdPersona().getUtenteList().stream().filter(u -> (u.getIdAzienda().getId().equals(struttura.getIdAzienda().getId()) && u.getAttivo())).toList();
                if (utentiDiAziendaDiStruttura != null && !utentiDiAziendaDiStruttura.isEmpty()) {
                    userPerQuery = utentiDiAziendaDiStruttura.get(0);
                }
            }
            return queryFactory
                .select(qUtenteStruttura)
                .from(qUtenteStruttura)
                .where(qUtenteStruttura.idUtente.id.eq(userPerQuery.getId())
                    .and(qUtenteStruttura.idStruttura.id.eq(struttura.getId())).and(qUtenteStruttura.attivo))
                .fetchFirst();
        } else {
            return null;
        }
    }

    public static AfferenzaStruttura getAfferenzaFromSigla(JPAQueryFactory queryFactory, String sigla, Utente utente) {
        AfferenzaStruttura.CodiciAfferenzaStruttura codice;
        boolean utenteHaAfferenzaDiretta = false;
        if (utente.getUtenteStrutturaList() != null) {
            utenteHaAfferenzaDiretta = !utente.getUtenteStrutturaList().stream().filter(us -> us.getAttivo() && us.getIdAfferenzaStruttura().getCodice().equals(AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA)).toList().isEmpty();
        }
        if (utenteHaAfferenzaDiretta && !sigla.equalsIgnoreCase("U")) {
            codice = AfferenzaStruttura.CodiciAfferenzaStruttura.FUNZIONALE;
        } else {
            switch (sigla) {
                case "F", "f" ->
                    codice = AfferenzaStruttura.CodiciAfferenzaStruttura.FUNZIONALE;
                case "T", "t" ->
                    codice = AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA;
                case "U", "u" -> {
                    codice = AfferenzaStruttura.CodiciAfferenzaStruttura.UNIFICATA;
                    break;
                }
                default -> {
                    return null;
                }
            }
        }
        AfferenzaStruttura afferenza = queryFactory
            .select(qffAfferenzaStruttura)
            .from(qffAfferenzaStruttura)
            .where(qffAfferenzaStruttura.codice.eq(codice.toString())).fetchOne();
        return afferenza;
    }

    public static void chiudiUtenteStruttura(DatiImportatiAppartenente entitaDaChiudere, Struttura strutturaSuCuiSpentereUtente, Integer idAziendaDestinazione, JPAQueryFactory queryFactory, PermissionManager permissionManager, EntityManager entityManager, List<UtenteStruttura> utenteStrutturaDaChiudereList) {
        Persona persona = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaChiudere.getCodiceFiscale()).and(qPersona.attiva)).fetchFirst();
        if (persona != null) {
            Utente utente = OperationsUtils.getUtenteDiIdAzienda(queryFactory, idAziendaDestinazione, persona);//                    strutturaAppartenteOriginale = OperationsUtils.getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, entitaDaInserire.getIdCasella(), entitaDaInserire.getIdAzienda(), qStruttura);
            //chiudere in baborg utenti struttura
            //chiudere in baborg utenti se non ci sono afferenze attive
            //chiudere in baborg persone se non ci sono utenti attivi
            UtenteStruttura utenteStruttura = getUtenteStrutturaAttivoByidCasella(queryFactory, strutturaSuCuiSpentereUtente, utente);
            if (utenteStruttura != null) {
                utenteStruttura.setAttivo(Boolean.FALSE);
                utenteStruttura.setAttivoAl(ZonedDateTime.now());
                //spengni tutti i permessi veicolati
                try {
                    permissionManager.deletePermission(
                        persona,
                        null,
                        null,
                        null,
                        null,
                        null,
                        BlackBoxConstants.Ambito.SCRIPTA.toString(),
                        BlackBoxConstants.Tipo.ARCHIVIO.toString(),
                        "ribaltone",
                        utenteStruttura.getIdStruttura());
                    //spengo anche questi anche se ad oggi non abbiamo permessi veicolati sugli utenti
                    permissionManager.deletePermission(
                        utente,
                        null,
                        null,
                        null,
                        null,
                        null,
                        BlackBoxConstants.Ambito.SCRIPTA.toString(),
                        BlackBoxConstants.Tipo.ARCHIVIO.toString(),
                        "ribaltone",
                        utenteStruttura.getIdStruttura());
                } catch (BlackBoxPermissionException ex) {
                    throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                }
                if (!utenteHasOtherStrutture(queryFactory, utenteStruttura.getIdStruttura(), utente)) {
                    utente.setAttivo(false);
                    utente.setDataSpegnimento(ZonedDateTime.now());
                    //spegnere tutti i permessi utente
                    try {
                        String[] ambitiDaSpegnere = {
                            BlackBoxConstants.Ambito.PICO.toString(),
                            BlackBoxConstants.Ambito.DELI.toString(),
                            BlackBoxConstants.Ambito.DETE.toString()};
                        for (String ambitoDaSpegnere : ambitiDaSpegnere) {

                            permissionManager.deletePermission(
                                utente,
                                null,
                                null,
                                null,
                                null,
                                null,
                                ambitoDaSpegnere,
                                BlackBoxConstants.Tipo.FLUSSO.toString(),
                                "ribaltone"
                            );
                        }
                    } catch (BlackBoxPermissionException ex) {
                        throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                    }
                    if (!personaHasOtherUtenti(queryFactory, persona)) {
                        persona.setAttiva(Boolean.FALSE);
                        persona.setDataSpegnimento(ZonedDateTime.now());
                        utente.setIdPersona(persona);
                        //spegni tutti i permessi persona
                        try {
                            permissionManager.deletePermission(
                                persona,
                                null,
                                null,
                                null,
                                null,
                                null,
                                BlackBoxConstants.Ambito.SCRIPTA.toString(),
                                BlackBoxConstants.Tipo.ARCHIVIO.toString(),
                                "ribaltone");
                        } catch (BlackBoxPermissionException ex) {
                            throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                        }
                    }
                    utenteStruttura.setIdUtente(utente);
                }
                entityManager.persist(utenteStruttura);
                if (utenteStrutturaDaChiudereList != null) {
                    utenteStrutturaDaChiudereList.add(utenteStruttura);
                }
                //chiudere tutti i permessi di flusso e veicolati per la struttura di riferimento
                try {
                    String[] ambitiDaSpegnere = {
                        BlackBoxConstants.Ambito.PICO.toString(),
                        BlackBoxConstants.Ambito.DELI.toString(),
                        BlackBoxConstants.Ambito.DETE.toString()};
                    for (String ambitoDaSpegnere : ambitiDaSpegnere) {

                        permissionManager.deletePermission(
                            utente,
                            utenteStruttura.getIdStruttura(),
                            null,
                            null,
                            Boolean.FALSE,
                            Boolean.FALSE,
                            ambitoDaSpegnere,
                            BlackBoxConstants.Tipo.FLUSSO.toString(),
                            "ribaltone");
                    }
                } catch (BlackBoxPermissionException ex) {
                    throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                }
            }
        }
    }

    private static UtenteStruttura getUtenteStrutturaAttivoByidCasella(JPAQueryFactory queryFactory, Struttura struttura, Utente utente) {
        if (struttura != null && utente != null) {
            log.info("utente cf: " + utente.getIdPersona().getCodiceFiscale());
            log.info("utente id: " + utente.getId());
            if (struttura.getIdCasella() == null) {
                log.info("struttura id_casella replicata: " + struttura.getIdStrutturaReplicata().getIdCasella());
            } else {
                log.info("struttura id_casella: " + struttura.getIdCasella());
            }
            Utente userPerQuery = utente;
            if (!utente.getIdAzienda().getId().equals(utente.getIdAzienda().getId())) {
                List<Utente> utentiDiAziendaDiStruttura = utente.getIdPersona().getUtenteList().stream().filter(u -> (u.getIdAzienda().getId().equals(utente.getIdAzienda().getId()) && u.getAttivo())).toList();
                if (utentiDiAziendaDiStruttura != null && !utentiDiAziendaDiStruttura.isEmpty()) {
                    userPerQuery = utentiDiAziendaDiStruttura.get(0);
                }
            }
            return queryFactory
                .select(qUtenteStruttura)
                .from(qUtenteStruttura)
                .where(qUtenteStruttura.idUtente.id.eq(userPerQuery.getId())
                    .and(qUtenteStruttura.idStruttura.id.eq(struttura.getId())).and(qUtenteStruttura.attivo))
                .fetchFirst();
        } else {
            return null;
        }
    }

    private static Boolean utenteHasOtherStrutture(JPAQueryFactory queryFactory, Struttura struttura, Utente utente) {
        if (struttura != null && utente != null) {
            return queryFactory
                .select(qUtenteStruttura.id)
                .from(qUtenteStruttura)
                .where(qUtenteStruttura.idUtente.id.eq(utente.getId())
                    .and(qUtenteStruttura.attivo)).limit(1).fetchFirst() != null;
        } else {
            return null;
        }
    }

    private static Boolean personaHasOtherUtenti(JPAQueryFactory queryFactory, Persona persona) {
        if (persona != null) {
            return queryFactory
                .select(qUtente.id)
                .from(qUtente)
                .where(qUtente.idPersona.id.eq(persona.getId()).and(qUtente.attivo))
                .fetchFirst() != null;
        } else {
            return null;
        }
    }

    public static Map<Long, Map<KeyMapReplica, Object>> getMappaReplicheStrutture(RepositoryFactory repositoryFactory, String tabella, Integer idAzienda) {
        String sql = "";
        switch (tabella) {
            case "BABORG":
                sql = "SELECT * FROM baborg.get_caselle_with_replicas(?)";
                break;
            case "DA_IMPORTARE":
                sql = "SELECT * FROM ribaltone_dati.get_caselle_with_replicas_dati_da_importare(?)";
                break;
            case "IMPORTATE":
                sql = "SELECT * FROM ribaltone_dati.get_caselle_with_replicas_dati_importati(?)";
                break;
            default:
                return null;
        }
        return repositoryFactory.getJdbcTemplate().execute(sql, (PreparedStatementCallback<Map<Long, Map<KeyMapReplica, Object>>>) ps -> {
            ps.setLong(1, Long.valueOf(idAzienda));
            try (ResultSet rs = ps.executeQuery()) {
                Map<Long, Map<KeyMapReplica, Object>> result = new HashMap<>();
                while (rs.next()) {
                    Long idCasella = rs.getLong("id_casella");
                    Boolean hasReplica = rs.getBoolean("has_replica");
                    String replicheJson = rs.getString("repliche");
                    List<StrutturaUnificata> repliche = new ArrayList<>();
                    if (replicheJson != null) {
                        repliche = repositoryFactory.getObjectMapper().readValue(replicheJson, new TypeReference<List<StrutturaUnificata>>() {
                        });
                    }
                    // ancestor_list è un array SQL
                    java.sql.Array sqlArray = rs.getArray("ancestor_list");
                    Long[] ancestors = sqlArray != null ? (Long[]) sqlArray.getArray() : new Long[0];

                    Map<KeyMapReplica, Object> row = new HashMap<>();
//                    row.put("id_casella", idCasella);
//                    row.put("ancestor_list", Arrays.asList(ancestors));
//                    row.put("has_replica", hasReplica);
//                    row.put("su_id", suId);

                    row.put(KeyMapReplica.ID_CASELLA, idCasella);
                    row.put(KeyMapReplica.ANCESTOR_LIST, Arrays.asList(ancestors));
                    row.put(KeyMapReplica.HAS_REPLICA, hasReplica);
                    row.put(KeyMapReplica.REPLICHE, repliche);

                    result.put(idCasella, row);
                }
                return result;
            } catch (JsonProcessingException ex) {
                throw new RibaltoneHttpException("errore nella conversione delle unificazioni nella funzione getMappaReplicheStrutture");
            }
        });

    }

    public static void storicizzaUtenteStruttura(Struttura strutturaSuCuiModificare, DatiDaImportareAppartenente entitaDaModificare, JPAQueryFactory queryFactory, EntityManager entityManager, PermissionManager permissionManager, List<UtenteStruttura> utenteStrutturaDaInserireList) {
        Persona persona = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaModificare.getCodiceFiscale())).fetchFirst();
        if (persona != null && strutturaSuCuiModificare != null) {
            Utente utente = OperationsUtils.getUtenteDiIdAzienda(queryFactory, strutturaSuCuiModificare.getIdAzienda().getId(), persona);
//                    Struttura struttura = OperationsUtils.getStrutturaAttivaFromIdCasellaAndIdAzienda(queryFactory, entitaDaInserire.getIdCasella(), entitaDaInserire.getIdAzienda(), qStruttura);
            persona.setCognome(entitaDaModificare.getCognome());
            persona.setNome(entitaDaModificare.getNome());
            persona.setDescrizione(entitaDaModificare.getCognome() + " " + entitaDaModificare.getNome());

            if (utente != null) {
                utente.setUsername(entitaDaModificare.getUsername() == null ? entitaDaModificare.getCodiceFiscale() : entitaDaModificare.getUsername());
                utente.setIdPersona(persona);
                UtenteStruttura utenteStruttura = OperationsUtils.getUtenteStrutturaAttivo(queryFactory, strutturaSuCuiModificare, utente);
                if (utenteStruttura != null) {
                    //creo il nuovo utente struttura
                    UtenteStruttura utenteStrutturaNew = UtenteStruttura.clone(utenteStruttura);
                    //posso spostare il dettaglio contatto tanto è lo stesso
                    utenteStrutturaNew.setIdDettaglioContatto(utenteStruttura.getIdDettaglioContatto());
                    utenteStrutturaNew.setResponsabile(entitaDaModificare.getResponsabile());
                    //spengo il vecchio utente struttura
                    utenteStruttura.setAttivoAl(ZonedDateTime.now());
                    utenteStruttura.setAttivo(false);
                    utenteStruttura.setIdDettaglioContatto(null);
//                    utenteStruttura.setIdUtente(utente);
                    utenteStrutturaNew.setIdAfferenzaStruttura(OperationsUtils.getAfferenzaFromSigla(queryFactory, entitaDaModificare.getTipoAppartenenza(), utente));
                    entityManager.persist(utenteStruttura);
                    entityManager.persist(utenteStrutturaNew);
                    if (utenteStrutturaDaInserireList != null) {
                        utenteStrutturaDaInserireList.add(utenteStrutturaNew);
                    }
                    if (entitaDaModificare.getResponsabile()) {
                        try {
                            permissionManager.insertSimplePermission(
                                utente,
                                strutturaSuCuiModificare,
                                BlackBoxConstants.Predicato.FIRMA.toString(),
                                "ribaltone",
                                Boolean.FALSE,
                                Boolean.FALSE,
                                BlackBoxConstants.Ambito.PICO.toString(),
                                BlackBoxConstants.Tipo.FLUSSO.toString());
                            permissionManager.insertSimplePermission(
                                utente,
                                strutturaSuCuiModificare,
                                BlackBoxConstants.Predicato.REDIGE.toString(),
                                "ribaltone",
                                Boolean.FALSE,
                                Boolean.FALSE,
                                BlackBoxConstants.Ambito.PICO.toString(),
                                BlackBoxConstants.Tipo.FLUSSO.toString());
                        } catch (BlackBoxPermissionException ex) {
                            throw new RibaltoneHttpException("errore nella creazione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                        }
                    } else {
                        try {
                            permissionManager.deletePermission(
                                utente,
                                strutturaSuCuiModificare,
                                BlackBoxConstants.Predicato.FIRMA.toString(),
                                "ribaltone",
                                Boolean.FALSE,
                                Boolean.FALSE,
                                BlackBoxConstants.Ambito.PICO.toString(),
                                BlackBoxConstants.Tipo.FLUSSO.toString(),
                                "ribaltone");
                            permissionManager.deletePermission(
                                utente,
                                strutturaSuCuiModificare,
                                BlackBoxConstants.Predicato.REDIGE.toString(),
                                "ribaltone",
                                Boolean.FALSE,
                                Boolean.FALSE,
                                BlackBoxConstants.Ambito.PICO.toString(),
                                BlackBoxConstants.Tipo.FLUSSO.toString(),
                                "ribaltone");
                        } catch (BlackBoxPermissionException ex) {
                            throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                        }
                    }
                }
            }

        }
    }
}
