package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Top
 */
public class OperationsUtils {

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
        em.createQuery("SELECT ribaltone_utils.sposta_struttura(:id_struttura_vecchia, :id_struttura_nuova, :tipo_operazione, :text_data_trasformazione);")
                .setParameter("id_struttura_vecchia", idStrutturaSorgente)
                .setParameter("id_struttura_nuova", idStrutturaDestinazione)
                .setParameter("tipo_operazione", tipoOperazione)
                .setParameter("id_strutturtext_data_trasformazionea_vecchia", dataTrasformazioneStr)
                .getSingleResult();

    }
}
