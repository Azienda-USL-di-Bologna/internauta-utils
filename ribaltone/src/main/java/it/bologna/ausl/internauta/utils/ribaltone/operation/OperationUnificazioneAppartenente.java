package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CHIUSURA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Top
 */
public class OperationUnificazioneAppartenente extends Operation<DatiRibaltoneInterface> implements Serializable {

    public static class UnificazionePair implements Serializable {

        private StrutturaUnificata.TipoUnificazione tipo;
        private List<StrutturaUnificata> strutture;

        // Costruttore vuoto necessario per Jackson
        public UnificazionePair() {
        }

        public UnificazionePair(StrutturaUnificata.TipoUnificazione tipo, List<StrutturaUnificata> strutture) {
            this.tipo = tipo;
            this.strutture = strutture;
        }

        public StrutturaUnificata.TipoUnificazione getTipo() {
            return tipo;
        }

        public void setTipo(StrutturaUnificata.TipoUnificazione tipo) {
            this.tipo = tipo;
        }

        public List<StrutturaUnificata> getStrutture() {
            return strutture;
        }

        public void setStrutture(List<StrutturaUnificata> strutture) {
            this.strutture = strutture;
        }
    }
    private UnificazionePair pair;
    private List<UtenteStruttura> utenteStrutturaDaInserireList = new ArrayList();
    private List<UtenteStruttura> utenteStrutturaDaSpegnereList = new ArrayList();

    public OperationUnificazioneAppartenente(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager, UnificazionePair pair, Map<String, String> descrizioniAggiuntive) {
        super(azione, entitaCoinvolta, entityManager, descrizioniAggiuntive);
        this.pair = pair;
    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        JPAQueryFactory jPAQueryFactory = new JPAQueryFactory(getEntityManager());
        QStruttura qStruttura = QStruttura.struttura;
        List<StrutturaUnificata> strutturaUnificataList = pair.getStrutture();
        switch (getAzione()) {

            case INSERT -> {
                DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                for (StrutturaUnificata strutturaUnificata : strutturaUnificataList) {
                    Struttura strutturaDoveInserire = strutturaUnificata.getIdStrutturaSorgente().getIdCasella().equals(entitaDaInserire.getIdCasella()) ? strutturaUnificata.getIdStrutturaDestinazione() : strutturaUnificata.getIdStrutturaSorgente();
                    OperationsUtils.insertUtenteInStruttura(jPAQueryFactory, entitaDaInserire, strutturaDoveInserire, getEntityManager(), repositoryFactory.getPermissionManager(), utenteStrutturaDaInserireList);
//                    if (pair.getLeft().equals(StrutturaUnificata.TipoUnificazione.FUSIONE)) {
//                    } else if (pair.getLeft().equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
//
//                    }
                }
            }
            case CHIUSURA -> {
                DatiImportatiAppartenente entitaDaChiudere = (DatiImportatiAppartenente) getEntitaCoinvolta();
                for (StrutturaUnificata strutturaUnificata : strutturaUnificataList) {
                    Struttura strutturaSorgenteDiAziendaInCuiChiudere = strutturaUnificata.getIdStrutturaSorgente().getIdCasella().equals(entitaDaChiudere.getIdCasella()) ? strutturaUnificata.getIdStrutturaDestinazione() : strutturaUnificata.getIdStrutturaSorgente();
                    Struttura strutturaDiUtenteDaRimuovere = jPAQueryFactory.select(qStruttura).from(qStruttura).where(
                        qStruttura.idCasella.eq(entitaDaChiudere.getIdCasella())
                            .and(qStruttura.attiva)
                            .and(qStruttura.idAzienda.id.eq(strutturaSorgenteDiAziendaInCuiChiudere.getIdAzienda().getId()))
                    ).orderBy(qStruttura.id.desc()).fetchOne();
                    if (strutturaDiUtenteDaRimuovere != null) {
                        OperationsUtils.chiudiUtenteStruttura(entitaDaChiudere, strutturaDiUtenteDaRimuovere.getIdCasella(), strutturaDiUtenteDaRimuovere.getIdAzienda().getId(), jPAQueryFactory, repositoryFactory.getPermissionManager(), getEntityManager(), utenteStrutturaDaSpegnereList);
                    } else {
                        throw new RibaltoneHttpException("errore nella gestione degli utenti unificati");
                    }
                }

            }
            case EDIT -> {
                DatiDaImportareAppartenente entitaDaModificare = (DatiDaImportareAppartenente) getEntitaCoinvolta();

                // è il caso di utente che diventa o non è più responsabile,
                // quindi verificare i permessi di flusso
                for (StrutturaUnificata strutturaUnificata : strutturaUnificataList) {
                    Struttura strutturaSorgenteDiAziendaInCuiModicare = strutturaUnificata.getIdStrutturaSorgente().getIdCasella().equals(entitaDaModificare.getIdCasella()) ? strutturaUnificata.getIdStrutturaDestinazione() : strutturaUnificata.getIdStrutturaSorgente();
                    Struttura strutturaDaModificare = jPAQueryFactory.select(qStruttura).from(qStruttura).where(
                        qStruttura.idCasella.eq(entitaDaModificare.getIdCasella())
                            .and(qStruttura.attiva)
                            .and(qStruttura.idAzienda.id.eq(strutturaSorgenteDiAziendaInCuiModicare.getIdAzienda().getId()))
                    ).orderBy(qStruttura.id.desc()).fetchOne();
                    OperationsUtils.editUtenteStruttura(strutturaDaModificare, entitaDaModificare, jPAQueryFactory, getEntityManager(), repositoryFactory.getPermissionManager(), utenteStrutturaDaInserireList);
                }
            }
            default ->
                throw new AssertionError();
        }
    }

    public void menageContattoAppartenenteUnificato(RepositoryFactory repositoryFactory) {
        for (UtenteStruttura utenteStrutturaNew : utenteStrutturaDaInserireList) {
            DettaglioContatto idDettaglioContatto = utenteStrutturaNew.getIdDettaglioContatto();
            if (idDettaglioContatto != null) {
                idDettaglioContatto.setDescrizione(utenteStrutturaNew.getIdStruttura().getNome() + " [" + utenteStrutturaNew.getIdStruttura().getIdCasella().toString() + "] [" + utenteStrutturaNew.getIdStruttura().getIdAzienda().getNome() + "]");
                idDettaglioContatto.setPrincipale(utenteStrutturaNew.getIdAfferenzaStruttura().getCodice().equals(AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA));
                for (DettaglioContatto dettaglioContatto : idDettaglioContatto.getIdContatto().getDettaglioContattoList()) {
                    dettaglioContatto.setPrincipale(dettaglioContatto.getUtenteStruttura().getIdAfferenzaStruttura().getCodice().equals(AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA));
                    getEntityManager().persist(dettaglioContatto);
                }
                getEntityManager().persist(idDettaglioContatto);
            } else {
                //devo creare il dettaglio contatto
                idDettaglioContatto = utenteStrutturaNew.buildDettaglioContatto();
                getEntityManager().persist(idDettaglioContatto);

            }
        }
        for (UtenteStruttura utenteStrutturaOld : utenteStrutturaDaSpegnereList) {
            DettaglioContatto idDettaglioContatto = utenteStrutturaOld.getIdDettaglioContatto();
            if (idDettaglioContatto != null) {
                idDettaglioContatto.setEliminato(Boolean.TRUE);
                getEntityManager().persist(idDettaglioContatto);
            }
        }
    }

    public UnificazionePair getPair() {
        return pair;
    }

    public void setPair(UnificazionePair pair) {
        this.pair = pair;
    }

    @Override
    public DatiRibaltoneInterface.TipologiaCsv getTipo() {
        return DatiRibaltoneInterface.TipologiaCsv.UNIFICAZIONI_APPARTENENTI;
    }

}
