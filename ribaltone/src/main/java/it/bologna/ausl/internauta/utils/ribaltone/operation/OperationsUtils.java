package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CAMBIO_PADRE;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.RINOMINA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
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
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.INSERT;
import it.bologna.ausl.model.entities.baborg.AttributiStruttura;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.UnificazioneEseguita;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class OperationsUtils {

    private static final Logger log = LoggerFactory.getLogger(OperationsUtils.class);

    /**
     *
     * @param em
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
        EntityManager em,
        JPAQueryFactory queryFactory,
        Integer idAzienda,
        Integer idCasella,
        String descrizione,
        Integer idCasellaPadre,
        QStruttura qStruttura,
        HashMap<Integer, List<Integer>> struttureDaAggiornareConPadre
    ) {
        Azienda azienda = em.find(Azienda.class, idAzienda);
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

            //da trovare il padre se non c'è devo segnarmela e poi sistemarla
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
                em.persist(strutturaDaInserire);
                em.refresh(strutturaDaInserire);
                StoricoRelazione storicoRelazione = new StoricoRelazione();
                storicoRelazione.setAttivaDal(ZonedDateTime.now());
                storicoRelazione.setIdStrutturaPadre(idStrutturaPadre);
                storicoRelazione.setIdStrutturaFiglia(strutturaDaInserire);
                em.persist(storicoRelazione);
                em.refresh(storicoRelazione);
            } else {
                em.persist(strutturaDaInserire);
                em.refresh(strutturaDaInserire);
                strutturaDaInserire.getId();
                struttureDaAggiornareConPadre = putInMap(struttureDaAggiornareConPadre, strutturaDaInserire, idCasellaPadre);
            }
            //ho inserito una struttura quindi cerco se devo collegare qualcosa
            if (struttureDaAggiornareConPadre.containsKey(strutturaDaInserire.getIdCasella())) {
                queryFactory
                    .update(qStruttura)
                    .set(qStruttura.idStrutturaPadre, strutturaDaInserire)
                    .where(qStruttura.id.in(struttureDaAggiornareConPadre.get(strutturaDaInserire.getIdCasella()))).execute();

                struttureDaAggiornareConPadre.remove(strutturaDaInserire.getIdCasella());
            }
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
    private static HashMap<Integer, List<Integer>> putInMap(HashMap<Integer, List<Integer>> mappa, Struttura struttura, Integer idPadre) {
        if (struttura != null) {
            if (mappa == null) {
                mappa = new HashMap<>();
                List<Integer> arrayList = new ArrayList<>();
                arrayList.add(struttura.getId());
                mappa.put(idPadre, arrayList);
            } else {
                List<Integer> listaDiFigliDiIdPadre;
                if (mappa.containsKey(idPadre)) {
                    listaDiFigliDiIdPadre = mappa.get(idPadre);
                    listaDiFigliDiIdPadre.add(struttura.getId());

                } else {
                    listaDiFigliDiIdPadre = new ArrayList<>();
                    listaDiFigliDiIdPadre.add(struttura.getId());

                }
                mappa.put(idPadre, listaDiFigliDiIdPadre);
            }
        }
        return mappa;
    }

    /**
     *
     * @param idCasellaDaChiudere
     * @param idAzienda
     * @param queryFactory
     * @param qStruttura
     * @param qStoricoRelazione
     * @param qStrutturaUnificata
     * @param spegniUnificazione
     * @return struttura appena chiusa
     * @throws RibaltoneHttpException
     */
    public static Struttura chiudiStruttura(Integer idCasellaDaChiudere, Integer idAzienda, JPAQueryFactory queryFactory, QStruttura qStruttura, QStoricoRelazione qStoricoRelazione, QStrutturaUnificata qStrutturaUnificata, Boolean spegniUnificazione) throws RibaltoneHttpException {
        //chiudere su baborg strutture
        //chiudere su baborg storico relazione
        //chiudere su baborg strutture unificate
        List<Struttura> struttureDaChiudereList = queryFactory
            .select(qStruttura)
            .from(qStruttura)
            .where(qStruttura.attiva.and(qStruttura.idCasella.eq(idCasellaDaChiudere)).and(qStruttura.idAzienda.id.eq(idAzienda))).fetch();
        if (struttureDaChiudereList == null || struttureDaChiudereList.size() > 1) {
            throw new RibaltoneHttpException("trovate piu di una struttura da chiudere la cosa non puo avvenire la struttura con problemi è: " + idCasellaDaChiudere.toString());
        } else {
            //chiudere su baborg strutture
            Struttura strutturaDaChiudere = struttureDaChiudereList.get(0);
            long updatedRows = queryFactory
                .update(qStruttura)
                .set(qStruttura.attiva, false)
                .set(qStruttura.dataCessazione, ZonedDateTime.now())
                .where(qStruttura.id.eq(strutturaDaChiudere.getId()).and(qStruttura.idAzienda.id.eq(idAzienda))).execute();
            if (updatedRows > 0) {
                strutturaDaChiudere = queryFactory
                    .selectFrom(qStruttura)
                    .where(qStruttura.id.eq(strutturaDaChiudere.getId()))
                    .fetchOne();
            }

            //chiudere su baborg storico relazione
            queryFactory
                .update(qStoricoRelazione)
                .set(qStoricoRelazione.attivaAl, ZonedDateTime.now())
                .where(
                    qStoricoRelazione.idStrutturaFiglia.id.eq(strutturaDaChiudere.getId())
                        .and(qStoricoRelazione.idStrutturaPadre.id.eq(strutturaDaChiudere.getIdStrutturaPadre().getId()))
                ).execute();
            //chiudere su baborg strutture unificate
            if (spegniUnificazione) {
                queryFactory.update(qStrutturaUnificata)
                    .set(qStrutturaUnificata.dataDisattivazione, ZonedDateTime.now())
                    .where(
                        qStrutturaUnificata.idStrutturaSorgente.id.eq(strutturaDaChiudere.getId())
                            .or(qStrutturaUnificata.idStrutturaDestinazione.id.eq(strutturaDaChiudere.getId()))
                    ).execute();
            }
            return strutturaDaChiudere;
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
    public static Struttura getStrutturaFromIdCasellaAndIdAziendaAndAttiva(JPAQueryFactory queryFactory, Integer idCasella, Integer idAzienda, QStruttura qStruttura) {
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
                    strutturaCoinvolta = getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, datiDaImportareAppartenente.getIdCasella(), datiDaImportareAppartenente.getIdAzienda(), qStruttura);
                } else {
                    DatiImportatiAppartenente datiImportatiAppartenente = (DatiImportatiAppartenente) entitaCoinvolta;
                    strutturaCoinvolta = getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, datiImportatiAppartenente.getIdCasella(), datiImportatiAppartenente.getIdAzienda(), qStruttura);
                }
                break;
            case STRUTTURE:
                if (entitaCoinvolta.getClasse().equals(DatiDaImportareStruttura.class.getCanonicalName())) {
                    DatiDaImportareStruttura datiDaImportareStruttura = (DatiDaImportareStruttura) entitaCoinvolta;
                    strutturaCoinvolta = getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, datiDaImportareStruttura.getIdCasella(), datiDaImportareStruttura.getIdAzienda(), qStruttura);
                } else {
                    DatiImportatiStruttura datiImportatiStruttura = (DatiImportatiStruttura) entitaCoinvolta;
                    strutturaCoinvolta = getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, datiImportatiStruttura.getIdCasella(), datiImportatiStruttura.getIdAzienda(), qStruttura);
                }
                break;

            default:
                throw new AssertionError();
        }
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
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
    }

    /**
     * funzione che si occupa di gestire le unificazioni di strutture
     *
     * @param entityManager
     * @param listOfOperationStruttura
     */
    public static void manageUnificazioni(EntityManager entityManager, List<OperationStruttura> listOfOperationStruttura) {
        for (OperationStruttura operationStruttura : listOfOperationStruttura) {
            //prendo tutti i padri della struttura
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            List<Struttura> antenatiSorgentiDiReplicazione = new ArrayList<>();
            QStruttura qStruttura = QStruttura.struttura;
            switch (operationStruttura.getAzione()) {
                case INSERT -> {
                    DatiDaImportareStruttura entitaDaInserire = (DatiDaImportareStruttura) operationStruttura.getEntitaCoinvolta();
                    //prendo i padri della struttura aperta
                    sincronizzaStrutturaUnificazioneSeCoinvolta(entityManager, queryFactory, entitaDaInserire, antenatiSorgentiDiReplicazione, INSERT);
                }
                case CHIUSURA -> {
                    DatiImportatiStruttura entitaDaChiudere = (DatiImportatiStruttura) operationStruttura.getEntitaCoinvolta();
                    //prendo i padri della struttura chiusa
                    //se sono replicato da qualche parte mi devo spegnere anche li
                    QStruttura strutturaSub = new QStruttura("strutturaSub");
                    //faccio l'update per disattivare nel caso mi trovi
                    Struttura strutturaDaSpegnere = queryFactory.select(qStruttura).from(qStruttura).where(
                        qStruttura.attiva.and(
                            qStruttura.idStrutturaReplicata.id.eq(
                                //intanto trovo la mia replica che è la struttura che contiene me come
                                JPAExpressions
                                    .select(strutturaSub.id)
                                    .from(strutturaSub)
                                    .where(
                                        strutturaSub.attiva.eq(Boolean.FALSE)
                                            .and(strutturaSub.idCasella.eq(entitaDaChiudere.getIdCasella()))
                                            .and(strutturaSub.idAzienda.id.eq(entitaDaChiudere.getIdAzienda()))).limit(1)
                            )
                        )
                    ).fetchOne();
                    strutturaDaSpegnere.setAttiva(false);
                    strutturaDaSpegnere.setDataCessazione(ZonedDateTime.now());

                    QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
                    queryFactory
                        .select(qStoricoRelazione)
                        .from(qStoricoRelazione)
                        .where();

                }
                case CAMBIO_PADRE, RINOMINA -> {
                    DatiDaImportareStruttura entitaDaCambio = (DatiDaImportareStruttura) operationStruttura.getEntitaCoinvolta();
                    //prendo i padri della struttura chiusa
//                    antenatiSorgentiDiReplicazione.addAll(getStruttureAntenateAttiveONo(entityManager, entitaDaCambio.getIdCasella(), true, entitaDaCambio.getIdAzienda()));
//                    QStruttura strutturaSub = new QStruttura("strutturaSub");
//                    //faccio l'update per disattivare nel caso trovi il mio corrispettivo replicato
                    ////                    queryFactory.update(qStruttura).set(qStruttura.dataCessazione, ZonedDateTime.now()).set(qStruttura.attiva, false).where(
////                        qStruttura.idStrutturaReplicata.id.in(
////                            //intanto trovo tutte le idCaselle = entitaDaCambio.getIdCasella() spente sulla mia azienda e spengo tutte
////                            // le repliche di tutte queste idCaselle che sono strutture gia spente.
////                            JPAExpressions
////                                .select(strutturaSub.id)
////                                .from(strutturaSub)
////                                .where(
////                                    strutturaSub.attiva.eq(false)
////                                        .and(strutturaSub.idCasella.eq(entitaDaCambio.getIdCasella()))
////                                        .and(strutturaSub.idAzienda.id.eq(entitaDaCambio.getIdAzienda()))))
////                    ).execute();
//                    antenatiSorgentiDiReplicazione = new ArrayList<>();
                    sincronizzaStrutturaUnificazioneSeCoinvolta(entityManager, queryFactory, entitaDaCambio, antenatiSorgentiDiReplicazione, CAMBIO_PADRE);
                }

            }
        }
    }

    /**
     *
     * @param queryFactory
     * @param idCasella
     * @param attiva
     * @return lista ordinata per livello di antenati struttura (dalla radice
     * Direzione Generale fino ad arrivare a idCasella passata)
     */
    private static List<Struttura> getStruttureAntenateAttiveONo(EntityManager entityManager, Integer idCasella, Boolean attive, Integer idAzienda) {
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
    private static void sincronizzaStrutturaUnificazioneSeCoinvolta(EntityManager entityManager, JPAQueryFactory queryFactory, DatiDaImportareStruttura entitaDaInserire, List<Struttura> antenatiSorgentiDiReplicazione, OperationAnagrafica.Azione azione) {
        QStruttura qStruttura = QStruttura.struttura;
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
        antenatiSorgentiDiReplicazione.addAll(getStruttureAntenateAttiveONo(entityManager, entitaDaInserire.getIdCasella(), true, entitaDaInserire.getIdAzienda()));
        List<Integer> idsStrutture = antenatiSorgentiDiReplicazione.stream().map(struttura -> struttura.getId()).collect(Collectors.toList());
        //controllo se uno dei padri è coinvolti in una unificazione
        //deve essere attiva l'unificazione
        //se l'azione e INSERT e
        //se l'unificazione è una FUSIONE allora è impossibile fondersi (la fusione fonde due strutture non due alberature)
        //se l'unificazione è una REPLICA allora devo vedere se uno dei miei antenati è una sorgente e nel caso replicare da li
        //se l'azione è CAMBIO_PADRE o RINOMINA
        // se l'unificazione è una fusione devo vedere se il vecchio me era facente parte di una fusione e nel caso risistemarmi
        //se l'unificazione è una replica devo edere se il vecchio me era facente parte di una replica e nel caso rireplicarmi

        if (azione.equals(INSERT)) {
            List<StrutturaUnificata> replicazioniCoinvolte = queryFactory
                .select(qStrutturaUnificata)
                .from(qStruttura)
                .join(qStrutturaUnificata).on(qStruttura.id.eq(qStrutturaUnificata.idStrutturaSorgente.id))
                .where(
                    (qStrutturaUnificata.dataDisattivazione.isNull().or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
                        .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.REPLICA))
                        .and(qStrutturaUnificata.idStrutturaSorgente.id.in(idsStrutture)))
                .fetch();
            Boolean replica = false;
            for (StrutturaUnificata strutturaUnificata : replicazioniCoinvolte) {
                //per ogni struttura trovata vado a inserire la nuova struttrua se non c'è già
                //                        Struttura strDestinazioneDiReplicazione = strutturaUnificata.getIdStrutturaDestinazione();
                for (Struttura s : antenatiSorgentiDiReplicazione) {
                    if (s.getId().equals(strutturaUnificata.getIdStrutturaSorgente().getId())) {
                        //dal prossimo giro devo iniziare a replicare l'alberatura
                        replica = true;
                    } else if (replica) {
                        //controllo se sono gia stato replicato
                        Struttura strutturaGiaReplicata = queryFactory
                            .select(qStruttura)
                            .from(qStruttura)
                            .where(
                                qStruttura.attiva.and(qStruttura.idStrutturaReplicata.id.eq(s.getId()))).fetchOne();
                        if (strutturaGiaReplicata == null) {
                            //devo replicare la struttura
                            //prendo il padre che sara quello che ha idstrutturaReplicata di padre di s
                            Struttura strutturaPadreDiStrutturaDaReplicare = queryFactory
                                .select(qStruttura)
                                .from(qStruttura)
                                .where(
                                    qStruttura.attiva.and(qStruttura.idStrutturaReplicata.id.eq(s.getIdStrutturaPadre().getId()))).fetchOne();

                            AttributiStruttura attributiStruttura = new AttributiStruttura();
                            attributiStruttura.setIdTipologiaStruttura(s.getAttributiStruttura().getIdTipologiaStruttura());
                            Struttura strReplicata = new Struttura(
                                s.getCodice(),
                                s.getNome(),
                                ZonedDateTime.now(),
                                null,
                                true,
                                strutturaPadreDiStrutturaDaReplicare,
                                s.getIdCasella(),
                                strutturaPadreDiStrutturaDaReplicare.getIdCasella(),
                                s.getUfficio(),
                                attributiStruttura,
                                strutturaPadreDiStrutturaDaReplicare.getIdAzienda(),
                                false);
                            strReplicata.setIdStrutturaReplicata(s);
                            StoricoRelazione storicoRelazione = new StoricoRelazione();
                            storicoRelazione.setAttivaDal(ZonedDateTime.now());
                            storicoRelazione.setIdStrutturaFiglia(strReplicata);
                            storicoRelazione.setIdStrutturaPadre(strutturaPadreDiStrutturaDaReplicare);

                            entityManager.persist(storicoRelazione);
                        }
                    }
                }
            }
        } else if (azione.equals(RINOMINA)) {
            //sono nel caso del cambio padre o della rinomina in ogni caso potrei avere un trascorso e potrei essere sia sorgente e che destinazione di una fusione
            //sia replica che fusione
            //se sono replicatore
            //il mio vecchio id sara dentro a idstruttura replicata quindi devo trovare il mio vecchio me
            //altrimenti il mio vecchio id sara dentro idStrutturaReplicata
            //andare a cercare la vecchia replica
            //andare a creare la nuova struttura replica
            //chiamare lo sposta struttura tra vecchio id e quello appena creato
            //primo passo sono replica o sono fusione? o non sono nulla?
            //se sono replica o figlio di replica allora trovero che io
            //o uno dei miei antenati sta in un qualche idStrutturaReplicata
            //oppure ce l'ho valorizzato se qualcuno è replicato in me
            List<Struttura> struttureReplicheCoinvolteNellaRinomina = queryFactory
                .select(qStruttura)
                .from(qStruttura)
                .where(
                    (qStruttura.dataCessazione.isNull().or(qStruttura.dataCessazione.after(ZonedDateTime.now())))
                        .and(qStruttura.attiva
                            .and(qStruttura.idStrutturaReplicata.idCasella.eq(entitaDaInserire.getIdCasella()))))
                .fetch();

            if (!struttureReplicheCoinvolteNellaRinomina.isEmpty()) {
                //vuol dire che nella tabella delle unificazioni o sono sorgente o lo è un mio antenato

                Struttura strutturaNuovaDaCollegare = queryFactory
                    .select(qStruttura)
                    .from(qStruttura)
                    .where(qStruttura.attiva
                        .and(qStruttura.idCasella.eq(entitaDaInserire.getIdCasella()))
                        .and(qStruttura.idAzienda.codice.eq(entitaDaInserire.getCodiceAzienda()))
                    ).fetchOne();

                Struttura strutturaVecchiaDaScollegare = struttureReplicheCoinvolteNellaRinomina.get(0).getIdStrutturaReplicata();

                if (strutturaVecchiaDaScollegare != null) {
                    List<Struttura> struttureAntenateAttiveONo = getStruttureAntenateAttiveONo(entityManager, entitaDaInserire.getIdCasella(), Boolean.TRUE, strutturaVecchiaDaScollegare.getIdAzienda().getId());

                    List<StrutturaUnificata> replicheDaValutare = queryFactory
                        .select(qStrutturaUnificata)
                        .from(qStrutturaUnificata)
                        .where(qStrutturaUnificata.dataAttivazione.before(ZonedDateTime.now())
                            .and(
                                qStrutturaUnificata.dataDisattivazione.isNull()
                                    .or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
                            .and(qStrutturaUnificata.idStrutturaSorgente.in(struttureAntenateAttiveONo))
                        ).fetch();
                    boolean accendiNuovaUnificazione = false;
                    for (StrutturaUnificata replica : replicheDaValutare) {
                        if (replica.getIdStrutturaSorgente().getIdCasella().equals(entitaDaInserire.getIdCasella())) {
                            //spegno questa e ne faccio una nuova per avere lo storico
                            replica.setDataDisattivazione(ZonedDateTime.now());
                            entityManager.persist(replica);
                            accendiNuovaUnificazione = true;
                            
                        }else{
                        //è un mio antenato a essere replicato quindi non devo fare nulla
                        }
                    }

                    for (Struttura idStrutturaDestinazioneVecchia : struttureReplicheCoinvolteNellaRinomina) {
                        
                        idStrutturaDestinazioneVecchia.setAttiva(false);
                        idStrutturaDestinazioneVecchia.setDataCessazione(ZonedDateTime.now());
                        entityManager.persist(idStrutturaDestinazioneVecchia);

                        Struttura idStrutturaDestinazioneNuova = new Struttura(
                            idStrutturaDestinazioneVecchia.getCodice(),
                            entitaDaInserire.getDescrizione(),
                            ZonedDateTime.now(),
                            null,
                            Boolean.TRUE,
                            idStrutturaDestinazioneVecchia.getIdStrutturaPadre(),
                            idStrutturaDestinazioneVecchia.getIdCasella(),
                            idStrutturaDestinazioneVecchia.getIdCasellaPadre(),
                            idStrutturaDestinazioneVecchia.getUfficio(),
                            idStrutturaDestinazioneVecchia.getAttributiStruttura(),
                            idStrutturaDestinazioneVecchia.getIdAzienda(),
                            idStrutturaDestinazioneVecchia.getSpettrale()
                        );
                        
                        if (accendiNuovaUnificazione){
                            List<StrutturaUnificata> replicheCorrelate = replicheDaValutare.stream().filter(u -> Objects.equals(u.getIdStrutturaDestinazione().getId(), idStrutturaDestinazioneVecchia.getId())).toList();
                            
                            for (StrutturaUnificata strutturaUnificata : replicheCorrelate) {
                                StrutturaUnificata nuovaUnificazione = new StrutturaUnificata();
                                nuovaUnificazione.setIdStrutturaDestinazione(idStrutturaDestinazioneNuova);
                                nuovaUnificazione.setIdStrutturaSorgente(strutturaNuovaDaCollegare);
                                nuovaUnificazione.setTipoOperazione(strutturaUnificata.getTipoOperazione());
                                nuovaUnificazione.setDataAttivazione(ZonedDateTime.now());
                                nuovaUnificazione.setDataInserimentoRiga(ZonedDateTime.now());
                                nuovaUnificazione.setDataAccensioneAttivazione(ZonedDateTime.now());
                                entityManager.persist(nuovaUnificazione);
                            }
                        }
                        
                        entityManager.persist(idStrutturaDestinazioneNuova);
                        //Gestisco lo storicoRelazione
                        gestisciStoricoRelazione(queryFactory, entityManager, idStrutturaDestinazioneNuova, idStrutturaDestinazioneVecchia);
                        //Gestisco gli UtentiStruttura
                        gestisciUtentiStruttura(queryFactory, entityManager, idStrutturaDestinazioneNuova, idStrutturaDestinazioneVecchia);
                        //sposta struttura
                        OperationsUtils.spostaStruttura(entityManager, idStrutturaDestinazioneVecchia.getId(), idStrutturaDestinazioneNuova.getId(), "R", idStrutturaDestinazioneNuova.getDataAttivazione().toString());
                    }
                }
            } else {
                //nessuno è replicato
            }
            
            //prendiamo in considerazione le fusioni non devo fare nulla perche 
            //ogni azienda ha nel proprio organigramma il nome di una struttura fusa
            //se lo gestiscono da organigramma 

//            List<StrutturaUnificata> fusioniCoinvolte = queryFactory
//                .select(qStrutturaUnificata)
//                .from(qStrutturaUnificata)
//                .where(
//                    (qStrutturaUnificata.dataDisattivazione.isNull().or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
//                        .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.FUSIONE))
//                        .and(qStrutturaUnificata.idStrutturaSorgente.idCasella.eq(entitaDaInserire.getIdCasella()).or(
//                            qStrutturaUnificata.idStrutturaDestinazione.idCasella.eq(entitaDaInserire.getIdCasella()))))
//                .fetch();
//
//            if (!fusioniCoinvolte.isEmpty()) {
//                // devo fare qualcosa
//                for (StrutturaUnificata unificazioneFusione : fusioniCoinvolte) {
//                    
//                }
//
//            } else {
//                //basta ho finito per questa struttura
//            }
//
//            QStruttura strutturaSub = new QStruttura("strutturaSub");
//            List<Struttura> struttureAntenate = queryFactory
//                .select(qStruttura)
//                .from(qStruttura)
//                .where(
//                    qStruttura.attiva
//                        .and(qStruttura.idStrutturaReplicata.id.in(
//                            JPAExpressions
//                                .select(strutturaSub.id)
//                                .from(strutturaSub)
//                                .where(strutturaSub.idCasella.eq(entitaDaInserire.getIdCasella()))
//                        ))).fetch();
//            for (Struttura strutturaReplica : struttureRepliche) {
//                //la spengo poi creo la nuova
//                strutturaReplica.setAttiva(false);
//                strutturaReplica.setDataCessazione(ZonedDateTime.now());
//            }
//
////            QStruttura strutturaSub = new QStruttura("strutturaSub");
//            //caso fusione
//            List<StrutturaUnificata> fusioniCoinvolte = queryFactory
//                .select(qStrutturaUnificata)
//                .from(qStruttura)
//                .join(qStrutturaUnificata).on(qStruttura.id.eq(qStrutturaUnificata.idStrutturaSorgente.id))
//                .where((qStrutturaUnificata.dataDisattivazione.isNull().or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
//                    .and(
//                        qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.FUSIONE)
//                    )
//                    .and(
//                        qStrutturaUnificata.idStrutturaSorgente.id.eq(
//                            JPAExpressions.select(strutturaSub.id).from(strutturaSub)
//                                .where(strutturaSub.attiva.eq(false)
//                                    .and(strutturaSub.idCasella.eq(entitaDaInserire.getIdCasella()))
//                                    .and(strutturaSub.idAzienda.id.eq(entitaDaInserire.getIdAzienda()))
//                                )
//                        ).or(qStrutturaUnificata.idStrutturaDestinazione.id.eq(
//                            JPAExpressions.select(strutturaSub.id).from(strutturaSub)
//                                .where(strutturaSub.attiva.eq(false)
//                                    .and(strutturaSub.idCasella.eq(entitaDaInserire.getIdCasella()))
//                                    .and(strutturaSub.idAzienda.id.eq(entitaDaInserire.getIdAzienda()))
//                                )
//                        ))
//                    )
//                ).fetch();
        }
    }

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

    private static void gestisciUtentiStruttura(JPAQueryFactory queryFactory, EntityManager entityManager, Struttura idStrutturaDestinazioneNuova, Struttura idStrutturaDestinazioneVecchia) {
        QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;

        List<UtenteStruttura> utentiStrutturaDaAggiornare = queryFactory
            .select(qUtenteStruttura)
            .from(qUtenteStruttura)
            .where(
                qUtenteStruttura.attivo.and(
                    qUtenteStruttura.idStruttura.id.eq(idStrutturaDestinazioneVecchia.getId())
                        .and(qUtenteStruttura.attivoAl.isNull().or(qUtenteStruttura.attivoAl.after(ZonedDateTime.now())))
                        .and(qUtenteStruttura.attivoDal.before(ZonedDateTime.now()))
                )
            ).fetch();
        for (UtenteStruttura utenteStrutturaDaAggiornare : utentiStrutturaDaAggiornare) {
            utenteStrutturaDaAggiornare.setAttivoAl(ZonedDateTime.now());
            utenteStrutturaDaAggiornare.setAttivo(Boolean.FALSE);

            UtenteStruttura utenteStrutturaNew = UtenteStruttura.clone(utenteStrutturaDaAggiornare);
            utenteStrutturaNew.setIdStruttura(idStrutturaDestinazioneNuova);
            utenteStrutturaNew.setAttivoDal(ZonedDateTime.now());
            utenteStrutturaNew.setAttivo(Boolean.TRUE);
            entityManager.persist(idStrutturaDestinazioneNuova);
            entityManager.persist(utenteStrutturaDaAggiornare);

        }
    }
}
