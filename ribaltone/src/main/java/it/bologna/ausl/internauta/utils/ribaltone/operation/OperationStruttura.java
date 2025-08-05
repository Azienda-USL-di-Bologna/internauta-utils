package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CAMBIO_PADRE;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CHIUSURA;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.INSERT;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.RINOMINA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public OperationStruttura(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager) {
        super(azione, entitaCoinvolta, entityManager);
    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        //string = codiceCasellaPadre che sto aspettando di inserire
        //List<Integer> = lista di id di strutture figlie che ho gia inserito e sulle quali devo fare update in idStrutturaPadre
        //dovro anche andare a ad inserire in storico relazione la riga
        if (workToDo == null) {
            workToDo = new HashMap<Integer, List<Integer>>();
        }
        HashMap<Integer, List<Integer>> struttureDaAggiornareConPadreNonAncoraInserito = (HashMap<Integer, List<Integer>>) workToDo;
        EntityManager em = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QStruttura qStruttura = QStruttura.struttura;
        QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
        List<Integer> idAziendeList = new ArrayList<>();
        List<Struttura> struttureUnificate = new ArrayList<>();
        switch (getAzione()) {
            case INSERT: {
                DatiDaImportareStruttura entitaDaInserire = (DatiDaImportareStruttura) getEntitaCoinvolta();
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
            }

            //ora gestisco il caso in cui inserisco la struttura e tocco un'unificazione
            break;

            case CHIUSURA: {
                //non serve spegnere i permessi veicolati qui perche tanto gli utenti
                //che facevano parte della struttura chiusa o non potranno entrare o
                //verranno spostati su altra struttura quindi questa operazione si fa negli utenti
                DatiImportatiStruttura entitaDaChiudere = (DatiImportatiStruttura) getEntitaCoinvolta();
                Azienda idAzienda = em.find(Azienda.class, entitaDaChiudere.getIdAzienda());
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
            }
            break;

            case CAMBIO_PADRE:
            case RINOMINA:
                String operazione = getAzione().equals(RINOMINA) ? "R" : "T";
                DatiDaImportareStruttura entitaDaCambio = (DatiDaImportareStruttura) getEntitaCoinvolta();
                //chiudere su baborg strutture old
                //chiudere su baborg storico relazione old
                Struttura strutturaSorgenteDaChiudere = queryFactory
                    .select(qStruttura)
                    .from(qStruttura)
                    .where(qStruttura.attiva.and(
                        qStruttura.idCasella.eq(entitaDaCambio.getIdCasella())).and(
                        qStruttura.idAzienda.id.eq(entitaDaCambio.getIdAzienda()))
                    ).fetchOne();

                strutturaChiusa = OperationsUtils.chiudiStruttura(
                    strutturaSorgenteDaChiudere,
                    queryFactory,
                    qStruttura,
                    qStoricoRelazione);

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

                OperationsUtils.attivaUtentiStruttura(queryFactory, em, strutturaNew, strutturaSorgenteDaChiudere);
                //OperationsUtils.inserisciStrutturaNewInAziendaUnificata(queryFactory, em, strutturaNew, struttureOld, getAzione());
                OperationsUtils.spostaStruttura(em, strutturaSorgenteDaChiudere.getId(), strutturaNew.getId(), operazione, strutturaNew.getDataAttivazione().toString());
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
        QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;
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
