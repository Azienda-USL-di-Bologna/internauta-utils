package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CAMBIO_PADRE;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CHIUSURA;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CONFLUENZA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.QStrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

/**
 *
 * @author Top
 */
public class OperationUnificazione extends Operation<DatiRibaltoneInterface> implements Serializable {

    private StrutturaUnificata.TipoUnificazione tipoUnificazione;

    public OperationUnificazione(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager, StrutturaUnificata.TipoUnificazione tipoFusione) {
        super(azione, entitaCoinvolta, entityManager);
        tipoUnificazione = tipoFusione;
    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(getEntityManager());
        QStrutturaUnificata qStrutturaUnificata = QStrutturaUnificata.strutturaUnificata;

        switch (getAzione()) {

            case INSERT -> {
                DatiDaImportareStruttura strutturaInserita = (DatiDaImportareStruttura) getEntitaCoinvolta();
                if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                    List<Struttura> struttureAntenateAttiveONo = OperationsUtils.getStruttureAntenateAttiveONo(getEntityManager(), strutturaInserita.getIdCasella(), true, strutturaInserita.getIdAzienda());
                    List<StrutturaUnificata> replicheSuCuiDevoCopiareStruttura = jPAQueryFactory.select(qStrutturaUnificata)
                        .from(qStrutturaUnificata).where(
                        qStrutturaUnificata.idStrutturaSorgente.in(struttureAntenateAttiveONo)
                            .and(qStrutturaUnificata.dataAttivazione.before(ZonedDateTime.now()))
                            .and(qStrutturaUnificata.dataDisattivazione.isNull()
                                .or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
                            .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.REPLICA))
                    ).fetch();
                    for (StrutturaUnificata strutturaUnificata : replicheSuCuiDevoCopiareStruttura) {
                        // per ogni destinazione devo creare la struttura se non trovo
                        //il padre devo inserire la struttura e poi segnarmelo come per le insert
                    }
                }
            }
            case CHIUSURA -> {
                DatiImportatiStruttura strutturaChiusa = (DatiImportatiStruttura) getEntitaCoinvolta();
                if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                    List<Struttura> struttureAntenateAttiveONo
                        = OperationsUtils.getStruttureAntenateAttiveONo(
                            getEntityManager(),
                            strutturaChiusa.getIdCasella(),
                            true,
                            strutturaChiusa.getIdAzienda()
                        );
                    List<StrutturaUnificata> replicheSuCuiDevoChiudereStruttura = jPAQueryFactory.select(qStrutturaUnificata)
                        .from(qStrutturaUnificata).where(
                        qStrutturaUnificata.idStrutturaSorgente.in(struttureAntenateAttiveONo)
                            .and(qStrutturaUnificata.dataAttivazione.before(ZonedDateTime.now()))
                            .and(qStrutturaUnificata.dataDisattivazione.isNull()
                                .or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
                            .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.REPLICA))
                    ).fetch();
                    for (StrutturaUnificata strutturaUnificata : replicheSuCuiDevoChiudereStruttura) {
                        // se sono la sorgente della replica devo spegnere l'unificazione
                        // per ogni destinazione devo andare a cercare nell'azienda la struttura e chiuderla
                        // devo fare la stressa cosa per gli utenti struttura
                        // devo fare la stessa cosa per gli utenti che non hanno piu strutture sull'azienda
                    }
                } else if (tipoUnificazione.equals(StrutturaUnificata.TipoUnificazione.FUSIONE)) {
                    List<StrutturaUnificata> fusioniSuCuiDevoChiudereStruttura = jPAQueryFactory.select(qStrutturaUnificata)
                        .from(qStrutturaUnificata).where(
                        qStrutturaUnificata.dataAttivazione.before(ZonedDateTime.now())
                            .and(qStrutturaUnificata.idStrutturaSorgente.id.eq(strutturaChiusa.getIdCasella())
                                .or(qStrutturaUnificata.idStrutturaDestinazione.id.eq(strutturaChiusa.getIdCasella())))
                            .and(qStrutturaUnificata.dataDisattivazione.isNull()
                                .or(qStrutturaUnificata.dataDisattivazione.after(ZonedDateTime.now())))
                            .and(qStrutturaUnificata.tipoOperazione.eq(StrutturaUnificata.TipoUnificazione.FUSIONE))
                    ).fetch();
                    for (StrutturaUnificata strutturaUnificata : fusioniSuCuiDevoChiudereStruttura) {
                        //devo chiudere la fusione
                        //devo togliere gli utenti con afferenza unificata che sono in struttura sorgente
                        //per ogni utente struttura spento controllo che l'utente abbia ancora senso se no lo spengo
                        //poi devo fare la stessa cosa per la destinazione
                    }
                }
            }
            case CAMBIO_PADRE -> {
                /*
                 * // * Premesso che ho fatto le trasformaizoni delle unificaizoni, e quindi sulla tbaella delle unifichazioni tutte le unificaizoni attive riguardano strutture attive
                 * // *
                 * // * NB: Escludiamo la gestione del caso in cui un trasferimento comporti l'inserimetno di una struttura già sorgente di replica come figlia di una struttura anch'essa sorgente di replica.
                 * // * Questo caso comporterebbe la doppia replica di una struttura. Essendo caso raro e di difficile gestione lo trascuriamo.
                 * // * Se accaddesse, dovrebbe essere trattabile a mano spegnendo una delle due unficazioni e lanciando uno sposta strutture tra la replica spenta e la replcia rimasta accesa
                 * // *
                 * // * cambio padre:
                 * // * - nel caso di fusioni devo solo aggiornare la fusione quindi non faccio nulla Lo sposta strutture delle unificazioni ha già sistemato la tabella unificaizoni
                 * // * - nel caso di repliche:
                 * // * -- Se sono sorgente di replica non faccio nulla. Lo sposta strutture delle unificazioni ha già sistemato la tabella unificaizoni
                 * // * -- Se non sorgente allora:
                 * // * - Query1: Per cominciare spengo tutte le strutture accese che abbiano come id_struttura_replciata il mio id_struttura_vecchio, mi faccio tornare anche l'id e l'id_azienda della struttura spenta
                 * // * - Se come id_struttura_nuovo sono un discendente di replica allora mi replico dove devo:
                 * // * -- Se dalla Query1 ho l'id vecchio (dell'azienda corretta) allora faccio lo sposta strutture
                 * // */
                DatiDaImportareStruttura strutturaTrasferita = (DatiDaImportareStruttura) getEntitaCoinvolta();
            }
            case RINOMINA -> {
                //sono nel caso del cambio padre o della rinomina in ogni caso potrei avere
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
                DatiDaImportareStruttura strutturaRinominata = (DatiDaImportareStruttura) getEntitaCoinvolta();
            }

            case CONFLUENZA -> {
                //questo dovrebbe essere come il caso della chiusura quando confluisco una struttura le sue fusioni cessano di esistere
                DatiImportatiStruttura strutturaConfluita = (DatiImportatiStruttura) getEntitaCoinvolta();
            }
            default ->
                throw new AssertionError();
        }
    }

    public void menageContatto(RepositoryFactory repositoryFactory) {

    }

    public StrutturaUnificata.TipoUnificazione getTipoUnificazione() {
        return tipoUnificazione;
    }

    public void setTipoUnificazione(StrutturaUnificata.TipoUnificazione tipoUnificazione) {
        this.tipoUnificazione = tipoUnificazione;
    }

    @Override
    public DatiRibaltoneInterface.TipologiaCsv getTipo() {
        return DatiRibaltoneInterface.TipologiaCsv.UNIFICAZIONI;
    }

}
