package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CAMBIO_PADRE;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CHIUSURA;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.INSERT;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.RINOMINA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.StoricoRelazione;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class OperationStruttura extends Operation<DatiRibaltoneInterface> implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(OperationStruttura.class);
    private Struttura strutturaNew;
    private Struttura strutturaChiusa;

    public OperationStruttura(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager, Map<String, String> descrizioniAggiuntive) {
        super(azione, entitaCoinvolta, entityManager, descrizioniAggiuntive);
    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        //string = codiceCasellaPadre che sto aspettando di inserire
        //List<Integer> = lista di id di strutture figlie che ho gia inserito e sulle quali devo fare update in idStrutturaPadre
        //dovro anche andare a ad inserire in storico relazione la riga
//        if (workToDo == null) {
//            workToDo = new HashMap<Integer, List<Integer>>();
//        }
        HashMap<Integer, List<Integer>> struttureDaAggiornareConPadreNonAncoraInserito = (HashMap<Integer, List<Integer>>) workToDo;
        EntityManager em = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QStruttura qStruttura = QStruttura.struttura;
        QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
//        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
//        List<Integer> idAziendeList = new ArrayList<>();
//        List<Struttura> struttureUnificate = new ArrayList<>();
        switch (getAzione()) {
            case INSERT:
                DatiDaImportareStruttura entitaDaInserire = (DatiDaImportareStruttura) getEntitaCoinvolta();
                log.info("sto gestendo inserimento struttura con id_casella = " + entitaDaInserire.getIdCasella());
                strutturaNew = OperationsUtils.inserisciStruttura(
                    em,
                    queryFactory,
                    entitaDaInserire.getIdAzienda(),
                    entitaDaInserire.getIdCasella(),
                    entitaDaInserire.getDescrizione(),
                    entitaDaInserire.getIdPadre(),
                    qStruttura,
                    struttureDaAggiornareConPadreNonAncoraInserito
                );
                break;
            //ora gestisco il caso in cui inserisco la struttura e tocco un'unificazione

            case CHIUSURA:
                //non serve spegnere i permessi veicolati qui perche tanto gli utenti
                //che facevano parte della struttura chiusa o non potranno entrare o
                //verranno spostati su altra struttura quindi questa operazione si fa negli utenti
                DatiImportatiStruttura entitaDaChiudere = (DatiImportatiStruttura) getEntitaCoinvolta();
                log.info("sto gestendo chiusura struttura con id_casella = " + entitaDaChiudere.getIdCasella());
//                Azienda idAzienda = em.find(Azienda.class, entitaDaChiudere.getIdAzienda());
                //chiudere su baborg strutture
                //chiudere su baborg storico relazione
                //chiudere su baborg strutture unificate
                Struttura strutturaSorgenteDaChiudere = queryFactory
                    .select(qStruttura)
                    .from(qStruttura)
                    .where(qStruttura.attiva.and(
                        qStruttura.idCasella.eq(entitaDaChiudere.getIdCasella())).and(
                        qStruttura.idAzienda.id.eq(entitaDaChiudere.getIdAzienda()))
                    ).fetchOne();

                strutturaChiusa = OperationsUtils.chiudiStruttura(strutturaSorgenteDaChiudere, queryFactory, qStruttura, qStoricoRelazione);

                try {
                    repositoryFactory.getPermissionManager().deletePermissionByObject(strutturaChiusa, null, null, null, null, BlackBoxConstants.Ambito.PICO.toString(), BlackBoxConstants.Tipo.FLUSSO.toString(), "ribaltone");
                    repositoryFactory.getPermissionManager().deletePermissionByObject(strutturaChiusa, null, null, null, null, BlackBoxConstants.Ambito.DELI.toString(), BlackBoxConstants.Tipo.FLUSSO.toString(), "ribaltone");
                    repositoryFactory.getPermissionManager().deletePermissionByObject(strutturaChiusa, null, null, null, null, BlackBoxConstants.Ambito.DETE.toString(), BlackBoxConstants.Tipo.FLUSSO.toString(), "ribaltone");
                    //todo chiudere i permessi veicolati
                    repositoryFactory.getPermissionManager().deleteVeicoledPermission(strutturaChiusa, "ribaltone");
                } catch (BlackBoxPermissionException ex) {
                    log.error("non sono stati rimossi i permessi di struttura con id " + strutturaChiusa.getId());
                }

                break;

            case CAMBIO_PADRE:
            case RINOMINA:
                String operazione = getAzione().equals(RINOMINA) ? "R" : "T";
                DatiDaImportareStruttura entitaDaCambio = (DatiDaImportareStruttura) getEntitaCoinvolta();
                log.info("sto gestendo " + operazione + " struttura con id_casella = " + entitaDaCambio.getIdCasella());
                //chiudere su baborg strutture old
                //chiudere su baborg storico relazione old
                Struttura strutturaSorgenteDaChiudereR = queryFactory
                    .select(qStruttura)
                    .from(qStruttura)
                    .where(qStruttura.attiva.and(
                        qStruttura.idCasella.eq(entitaDaCambio.getIdCasella())).and(
                        qStruttura.idAzienda.id.eq(entitaDaCambio.getIdAzienda()))
                    ).fetchOne();

                strutturaChiusa = OperationsUtils.chiudiStruttura(
                    strutturaSorgenteDaChiudereR,
                    queryFactory,
                    qStruttura,
                    qStoricoRelazione);
                getEntityManager().refresh(strutturaChiusa);
                //Inserire su baborg strutture new
                //Inserire su baborg storico relazione new
                strutturaNew = OperationsUtils.inserisciStruttura(
                    em,
                    queryFactory,
                    entitaDaCambio.getIdAzienda(),
                    entitaDaCambio.getIdCasella(),
                    entitaDaCambio.getDescrizione(),
                    entitaDaCambio.getIdPadre(),
                    qStruttura,
                    struttureDaAggiornareConPadreNonAncoraInserito
                );
                getEntityManager().refresh(strutturaNew);
                //se sono nel caso di rinomina della radice (e non solo)devo aggiornare anche gli storici relazione di tutti quelli che sono collegati a me
                queryFactory
                    .update(qStruttura)
                    .set(qStruttura.idStrutturaPadre, strutturaNew)
                    .where(qStruttura.idStrutturaPadre.id.eq(strutturaChiusa.getId()).and(qStruttura.attiva.eq(Boolean.TRUE)))
                    .execute();
                List<StoricoRelazione> storiciRelazioneDaChiudereERiaprire = queryFactory.select(qStoricoRelazione).from(qStoricoRelazione).where(qStoricoRelazione.idStrutturaPadre.id.eq(strutturaChiusa.getId())).fetch();
                for (StoricoRelazione storicoRelazione : storiciRelazioneDaChiudereERiaprire) {
                    storicoRelazione.setAttivaAl(ZonedDateTime.now());
                    //lo sposta struttura si occupa anche di spostare gli uffici
                    if (!storicoRelazione.getIdStrutturaFiglia().getUfficio()) {
                        StoricoRelazione storicoRelazioneNew = new StoricoRelazione();
                        storicoRelazioneNew.setAttivaDal(ZonedDateTime.now());
                        storicoRelazioneNew.setIdStrutturaFiglia(storicoRelazione.getIdStrutturaFiglia());
                        storicoRelazioneNew.setIdStrutturaPadre(strutturaNew);

                        getEntityManager().persist(storicoRelazione);
                        getEntityManager().persist(storicoRelazioneNew);
                        getEntityManager().flush();
                    }
                }
                OperationsUtils.inserisciSpostaUtentiStruttura(queryFactory, em, strutturaNew, strutturaSorgenteDaChiudereR);
                //OperationsUtils.inserisciStrutturaNewInAziendaUnificata(queryFactory, em, strutturaNew, struttureOld, getAzione());
                OperationsUtils.spostaStruttura(em, strutturaSorgenteDaChiudereR.getId(), strutturaNew.getId(), operazione, strutturaNew.getDataAttivazione().toString());
                break;
            default:
                throw new AssertionError();

        }
    }

    /**
     *
     * @param repositoryFactory
     * @throws RibaltoneHttpException
     */
    //nel caso di una rinomina e cambio padre della struttura l'idCasella non cambia quindi
    // le operazione non sono insert o chiusura le faccio nelle trasformazioni
    //in caso di rinomina andrò a cercare il contatto lo andrò a updatare con nuovo nome e nuovo puntamento
    //in caso di un cambio padre della struttura il nome resta il medesimo quindi solo nuovo puntamento
    //in caso di confluenza devo togliere dai gruppi il vecchio contatto e mettere quello della struttura conlfuita
    public void menageContattoStruttura(RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        QStruttura qStruttura = QStruttura.struttura;
        EntityManager em = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
//        QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;
        switch (getAzione()) {
            //va inserito il contatto
            case INSERT -> {
                DatiDaImportareStruttura entitaDaInserire = (DatiDaImportareStruttura) getEntitaCoinvolta();
                Struttura s = queryFactory.select(qStruttura).from(qStruttura).where(qStruttura.idCasella.eq(entitaDaInserire.getIdCasella()).and(qStruttura.idAzienda.id.eq(entitaDaInserire.getIdAzienda())).and(qStruttura.attiva)).fetchOne();
                if (s != null && s.getIdContatto() == null) {
                    QPersona qPersona = QPersona.persona;
                    Persona p = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq("RIBALTONE")).fetchOne();
                    Integer[] idAziende = new Integer[1];
                    idAziende[0] = s.getIdAzienda().getId();
                    Contatto buildContattoAndDettaglio = s.buildContattoAndDettaglio(p.getUtenteList().get(0), p, idAziende);
                    s.setIdContatto(buildContattoAndDettaglio);
                    em.persist(s);
                }
            }
            //va eliminato il contatto, ma non so ancora se sia dovuto ad una confluenza o meno quindi lo setto solo come da eliminare
            // puliro questa cosa durante la managecontatto delle trasformazioni
            case CHIUSURA -> {
                DatiImportatiStruttura entitaDaChiudere = (DatiImportatiStruttura) getEntitaCoinvolta();
                Struttura s = queryFactory.select(qStruttura).from(qStruttura)
                    .where(qStruttura.idCasella.eq(entitaDaChiudere.getIdCasella())
                        .and(qStruttura.idAzienda.id.eq(entitaDaChiudere.getIdAzienda()))
                        .and(qStruttura.attiva)).fetchOne();
                if (s != null) {
                    Contatto idContatto = s.getIdContatto();
                    idContatto.setEliminato(true);
                    s.setIdContatto(idContatto);
                    em.persist(s);
                }
            }
            // va modificato il nome e descrizione del contatto della struttura e dei dettagli degli us
            case RINOMINA -> {

            }
            //non devo fare nulla
            case CAMBIO_PADRE -> {
            }
            default ->
                throw new AssertionError();
        }
    }

}
