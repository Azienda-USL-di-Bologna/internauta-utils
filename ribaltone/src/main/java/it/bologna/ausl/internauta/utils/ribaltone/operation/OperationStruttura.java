package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CHIUSURA;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.RINOMINA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.QStoricoRelazione;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Top
 */
public class OperationStruttura extends Operation<DatiRibaltoneInterface> implements Serializable {

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
        EntityManager em = getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        QStruttura qStruttura = QStruttura.struttura;
        QStoricoRelazione qStoricoRelazione = QStoricoRelazione.storicoRelazione;
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;
        List<Integer> idAziendeList = new ArrayList<>();
        List<Struttura> struttureUnificate = new ArrayList<>();

        switch (getAzione()) {
            case INSERT: {
                DatiDaImportareStruttura entitaDaInserire = (DatiDaImportareStruttura) getEntitaCoinvolta();
                OperationsUtils.inserisciStruttura(
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
                OperationsUtils.chiudiStruttura(entitaDaChiudere.getIdCasella(), idAzienda.getId(), queryFactory, qStruttura, qStoricoRelazione, qStrutturaUnificata, true);
                //ora gestisco il caso in cui chiudo la struttura e tocco un'unificazione
            }
            break;

            case CAMBIO_PADRE:
            case RINOMINA:
                String operazione = getAzione().equals(RINOMINA) ? "R" : "T";
                DatiDaImportareStruttura entitaDaCambio = (DatiDaImportareStruttura) getEntitaCoinvolta();
                //chiudere su baborg strutture old
                //chiudere su baborg storico relazione old
                Struttura strutturaChiusa = OperationsUtils.chiudiStruttura(
                    entitaDaCambio.getIdCasella(),
                    entitaDaCambio.getIdAzienda(),
                    queryFactory,
                    qStruttura,
                    qStoricoRelazione,
                    qStrutturaUnificata,
                    false);

                //Inserire su baborg strutture new
                //Inserire su baborg storico relazione new
                Struttura strutturaAppenaInserita = OperationsUtils.inserisciStruttura(
                    em,
                    queryFactory,
                    entitaDaCambio.getIdAzienda(),
                    entitaDaCambio.getIdCasella(),
                    entitaDaCambio.getDescrizione(),
                    entitaDaCambio.getIdPadre(),
                    qStruttura,
                    struttureDaAggiornareConPadreNonAncoraInserito
                );
                //aggiustare unificazione
//                OperationsUtils.aggiustaUnificazioni(
//                        entitaDaCambio.getIdCasella(),
//                        strutturaAppenaInserita,
//                        em,
//                        queryFactory,
//                        qStrutturaUnificata);
                //spostaStrutture
                OperationsUtils.spostaStruttura(em, strutturaChiusa.getId(), strutturaAppenaInserita.getId(), operazione, strutturaAppenaInserita.getDataAttivazione().toString());

                //ora gestisco il caso in cui inserisco la struttura e tocco un'unificazione
                break;
            default:
                throw new AssertionError();
        }
    }

}
