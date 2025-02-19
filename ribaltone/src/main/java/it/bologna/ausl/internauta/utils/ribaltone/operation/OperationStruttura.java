package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CHIUSURA;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.RINOMINA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.baborg.AttributiStruttura;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Top
 */
public class OperationStruttura extends Operation<DatiRibaltoneInterface> implements Serializable {

    public OperationStruttura(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager) {
        super(azione, entitaCoinvolta, entityManager);
    }

    @Override
    public void esegui(Object workToDo) throws RibaltoneHttpException {
        //string = codiceCasellaPadre che sto aspettando di inserire
        //List<Integer> = lista di id di strutture figlie che ho gia inserito e sulle quali devo fare update in idStrutturaPadre
        //dovro anche andare a ad inserire in storico relazione la riga
        if (workToDo == null) {
            workToDo = new HashMap<Integer, List<Integer>>();
        }
        HashMap<Integer, List<Integer>> struttureDaAggiornareConPadre = (HashMap<Integer, List<Integer>>) workToDo;
        EntityManager em = getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QStruttura qStruttura = QStruttura.struttura;
        QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
        switch (getAzione()) {
            case INSERT: {
                DatiDaImportareStruttura entitaDaInserire = (DatiDaImportareStruttura) getEntitaCoinvolta();
                inserisciStruttura(
                        em,
                        queryFactory,
                        entitaDaInserire.getIdAzienda(),
                        entitaDaInserire.getIdCasella(),
                        entitaDaInserire.getDescrizione(),
                        entitaDaInserire.getIdPadre(),
                        qStruttura,
                        struttureDaAggiornareConPadre
                );
            }
            break;

            case CHIUSURA: {
                DatiImportatiStruttura entitaDaChiudere = (DatiImportatiStruttura) getEntitaCoinvolta();
                Azienda idAzienda = em.find(Azienda.class, entitaDaChiudere.getIdAzienda());
                //chiudere su baborg strutture
                //chiudere su baborg storico relazione
                //chiudere su baborg strutture unificate
                chiudiStruttura(entitaDaChiudere.getIdCasella(), idAzienda.getId(), queryFactory, qStruttura, qStoricoRelazione, qStrutturaUnificata, true);

            }
            break;

            case CAMBIO_PADRE:
            case RINOMINA:
                DatiDaImportareStruttura entitaDaCambio = (DatiDaImportareStruttura) getEntitaCoinvolta();
                //chiudere su baborg strutture old
                //chiudere su baborg storico relazione old
                chiudiStruttura(
                        entitaDaCambio.getIdCasella(),
                        entitaDaCambio.getIdAzienda(),
                        queryFactory, 
                        qStruttura, 
                        qStoricoRelazione, 
                        qStrutturaUnificata, 
                        false);

                //Inserire su baborg strutture new
                //Inserire su baborg storico relazione new
                Struttura strutturaAppenaInserita = inserisciStruttura(
                        em,
                        queryFactory,
                        entitaDaCambio.getIdAzienda(),
                        entitaDaCambio.getIdCasella(),
                        entitaDaCambio.getDescrizione(),
                        entitaDaCambio.getIdPadre(),
                        qStruttura,
                        struttureDaAggiornareConPadre
                );
                //aggiustare unificazione
                aggiustaUnificazioni(
                        entitaDaCambio.getIdCasella(), 
                        strutturaAppenaInserita,
                        em, 
                        queryFactory, 
                        qStrutturaUnificata);
                //spostaStrutture
                
            break;
//             {
//                DatiDaImportareStruttura entitaDaRinomina = (DatiDaImportareStruttura) getEntitaCoinvolta();
//                chiudiStruttura(entitaDaRinomina.getIdCasella(), entitaDaRinomina.getIdAzienda(), queryFactory, qStruttura, qStoricoRelazione, qStrutturaUnificata, false);
//                //Inserire su baborg strutture new
//                //Inserire su baborg storico relazione new
//                //chiudere su baborg strutture old
//                //chiudere su baborg storico relazione old
//                Struttura strutturaAppenaInserita = inserisciStruttura(
//                        em,
//                        queryFactory,
//                        entitaDaRinomina.getIdAzienda(),
//                        entitaDaRinomina.getIdCasella(),
//                        entitaDaRinomina.getDescrizione(),
//                        entitaDaRinomina.getIdPadre(),
//                        qStruttura,
//                        struttureDaAggiornareConPadre
//                );
//                //aggiustare unificazione
//                aggiustaUnificazioni(entitaDaRinomina.getIdCasella(), strutturaAppenaInserita, em, queryFactory, qStrutturaUnificata);
//                //spostaStrutture
//            }
//            break;

            default:
                throw new AssertionError();
        }
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
    private HashMap<Integer, List<Integer>> putInMap(HashMap<Integer, List<Integer>> mappa, Struttura struttura, Integer idPadre) {
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

    private void chiudiStruttura(Integer idCasellaDaChiudere, Integer idAzienda, JPAQueryFactory queryFactory, QStruttura qStruttura, QStoricoRelazione qStoricoRelazione, QStrutturaUnificata qStrutturaUnificata, Boolean spegniUnificazione) throws RibaltoneHttpException {
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
            queryFactory
                    .update(qStruttura)
                    .set(qStruttura.attiva, false)
                    .set(qStruttura.dataCessazione, ZonedDateTime.now())
                    .where(qStruttura.id.eq(strutturaDaChiudere.getId()).and(qStruttura.idAzienda.id.eq(idAzienda))).execute();
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
        }
    }

    public Struttura inserisciStruttura(
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
                    azienda
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

    private void aggiustaUnificazioni(Integer idCasellaDaChiudere, Struttura strutturaAppenaInserita, EntityManager em, JPAQueryFactory queryFactory, QStrutturaUnificata qStrutturaUnificata) {
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
}
