package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CHIUSURA;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class OperationUnificazioneAppartenente extends Operation<DatiRibaltoneInterface> implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(OperationAppartenente.class);

    public static class UnificazionePair implements Serializable {

        public enum DirezioneReplica {
            PASSATO,
            FUTURO
        }

        private StrutturaUnificata.TipoUnificazione tipo;
        private List<StrutturaUnificata> strutture;
        private DirezioneReplica direzioneReplica;

        // Costruttore vuoto necessario per Jackson
        public UnificazionePair() {
        }

        public UnificazionePair(StrutturaUnificata.TipoUnificazione tipo, List<StrutturaUnificata> strutture, DirezioneReplica direzioneReplica) {
            this.tipo = tipo;
            this.strutture = strutture;
            this.direzioneReplica = direzioneReplica;
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

        public DirezioneReplica getDirezioneReplica() {
            return direzioneReplica;
        }

        public void setDirezioneReplica(DirezioneReplica direzioneReplica) {
            this.direzioneReplica = direzioneReplica;
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
                    StrutturaUnificata strutturaUnificataReload = getEntityManager().find(StrutturaUnificata.class, strutturaUnificata.getId());
                    Struttura strutturaDoveInserire = null;
                    if (strutturaUnificataReload.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                        strutturaDoveInserire = jPAQueryFactory
                            .select(qStruttura)
                            .from(qStruttura)
                            .where(
                                qStruttura.attiva
                                    .and(qStruttura.idStrutturaReplicata.idCasella.eq(entitaDaInserire.getIdCasella()))
                                    .and(qStruttura.idAzienda.id.eq(strutturaUnificataReload.getIdStrutturaDestinazione().getIdAzienda().getId()))
                            ).fetchOne();
                    } else {
                        strutturaDoveInserire = strutturaUnificataReload.getIdStrutturaSorgente().getIdCasella().equals(entitaDaInserire.getIdCasella()) ? strutturaUnificataReload.getIdStrutturaDestinazione() : strutturaUnificataReload.getIdStrutturaSorgente();
                    }
                    log.info("inserisco utenteunificato " + entitaDaInserire.getCodiceFiscale() + " alla struttura con id " + strutturaDoveInserire.getId());
                    OperationsUtils.insertUtenteInStruttura(jPAQueryFactory, entitaDaInserire, strutturaDoveInserire, getEntityManager(), repositoryFactory.getPermissionManager(), utenteStrutturaDaInserireList);
                    UtenteStruttura utenteStrutturaInserito = utenteStrutturaDaInserireList.get(utenteStrutturaDaInserireList.size() - 1);

                    if (entitaDaInserire.getResponsabile()) {
                        Struttura strutturaPartenza;
                        if (strutturaUnificataReload.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                            strutturaPartenza = jPAQueryFactory
                                .select(qStruttura)
                                .from(qStruttura)
                                .where(
                                    qStruttura.attiva
                                        .and(qStruttura.idCasella.eq(entitaDaInserire.getIdCasella()))
                                        .and(qStruttura.idAzienda.id.eq(entitaDaInserire.getIdAzienda()))
                                ).fetchOne();
                        } else {
                            strutturaPartenza = strutturaUnificataReload.getIdStrutturaSorgente().getIdCasella().equals(entitaDaInserire.getIdCasella()) ? strutturaUnificataReload.getIdStrutturaSorgente() : strutturaUnificataReload.getIdStrutturaDestinazione();
                        }
                        try {
                            List<Utente> utenti = utenteStrutturaInserito.getIdUtente().getIdPersona().getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(strutturaPartenza.getIdAzienda().getId())).toList();
                            if (utenti != null && !utenti.isEmpty()) {
                                repositoryFactory.getPermissionManager().copyActiveFlowPermissionsFromSubjectObjectToSubjectObject(
                                    utenti.get(0),
                                    strutturaPartenza,
                                    utenteStrutturaInserito.getIdUtente(),
                                    strutturaDoveInserire
                                );
                            }
                        } catch (BlackBoxPermissionException ex) {
                            throw new RibaltoneHttpException("errore nel mettere i permessi a utente con cf " + entitaDaInserire.getCodiceFiscale(), ex);
                        }
                    }
                }
            }
            case CHIUSURA -> {
                DatiImportatiAppartenente entitaDaChiudere = (DatiImportatiAppartenente) getEntitaCoinvolta();
                for (StrutturaUnificata sU : strutturaUnificataList) {
                    StrutturaUnificata strutturaUnificata = getEntityManager().find(StrutturaUnificata.class, sU.getId());
//                    Struttura strutturaSorgenteDiAziendaInCuiChiudere = strutturaUnificata.getIdStrutturaSorgente().getIdCasella().equals(entitaDaChiudere.getIdCasella()) ? strutturaUnificata.getIdStrutturaDestinazione() : strutturaUnificata.getIdStrutturaSorgente();
                    Struttura strutturaDiUtenteDaRimuovere = strutturaUnificata.getIdStrutturaDestinazione();
                    if (strutturaUnificata.getTipoOperazione().equals(StrutturaUnificata.TipoUnificazione.REPLICA)) {
                        strutturaDiUtenteDaRimuovere = jPAQueryFactory
                            .select(qStruttura)
                            .from(qStruttura)
                            .where(
                                qStruttura.attiva
                                    .and(qStruttura.idStrutturaReplicata.idCasella.eq(entitaDaChiudere.getIdCasella()))
                                    .and(qStruttura.idAzienda.id.eq(strutturaUnificata.getIdStrutturaDestinazione().getIdAzienda().getId()))
                            ).fetchOne();

                    }
                    if (strutturaDiUtenteDaRimuovere != null) {
                        log.info("rimuovo utente unificato " + entitaDaChiudere.getCodiceFiscale() + " alla struttura con id " + strutturaDiUtenteDaRimuovere.getId());
                        OperationsUtils.chiudiUtenteStruttura(entitaDaChiudere, strutturaDiUtenteDaRimuovere, strutturaDiUtenteDaRimuovere.getIdAzienda().getId(), jPAQueryFactory, repositoryFactory.getPermissionManager(), getEntityManager(), utenteStrutturaDaSpegnereList);
                    }
                }

            }

            case EDIT -> {
                DatiDaImportareAppartenente entitaDaModificare = (DatiDaImportareAppartenente) getEntitaCoinvolta();

                // è il caso di utente che diventa o non è più responsabile,
                // quindi verificare i permessi di flusso
                for (StrutturaUnificata sU : strutturaUnificataList) {
                    StrutturaUnificata strutturaUnificata = getEntityManager().find(StrutturaUnificata.class, sU.getId());
                    getEntityManager().refresh(strutturaUnificata);
                    Struttura strutturaSorgenteDiAziendaInCuiModicare = strutturaUnificata.getIdStrutturaSorgente().getIdCasella().equals(entitaDaModificare.getIdCasella()) ? strutturaUnificata.getIdStrutturaDestinazione() : strutturaUnificata.getIdStrutturaSorgente();
                    Struttura strutturaDaModificare = jPAQueryFactory.select(qStruttura).from(qStruttura).where(
                        qStruttura.idStrutturaReplicata.idCasella.eq(entitaDaModificare.getIdCasella())
                            .and(qStruttura.attiva)
                            .and(qStruttura.idAzienda.id.eq(strutturaUnificata.getIdStrutturaDestinazione().getIdAzienda().getId()))
                    ).orderBy(qStruttura.id.desc()).fetchOne();
                    log.info("modifico utente unificato " + entitaDaModificare.getCodiceFiscale() + " alla struttura con id " + strutturaDaModificare.getId() + " responsabile " + entitaDaModificare.getResponsabile().toString());
                    OperationsUtils.editUtenteStruttura(strutturaDaModificare, entitaDaModificare, jPAQueryFactory, getEntityManager(), repositoryFactory.getPermissionManager(), utenteStrutturaDaInserireList);
                    QPersona qPersona = QPersona.persona;
                    Persona persona = jPAQueryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaModificare.getCodiceFiscale())).fetchFirst();
                    if (persona != null) {
                        try {
                            Utente utente = OperationsUtils.getUtenteDiIdAzienda(jPAQueryFactory, strutturaSorgenteDiAziendaInCuiModicare.getIdAzienda().getId(), persona);

                            if (utente != null) {
                                repositoryFactory.getPermissionManager().copyActiveFlowPermissionsFromSubjectObjectToSubjectObject(
                                    utente,
                                    strutturaSorgenteDiAziendaInCuiModicare,
                                    utenteStrutturaDaInserireList.get(utenteStrutturaDaInserireList.size() - 1).getIdUtente(),
                                    utenteStrutturaDaInserireList.get(utenteStrutturaDaInserireList.size() - 1).getIdStruttura()
                                );
                            }
                        } catch (BlackBoxPermissionException ex) {
                            throw new RibaltoneHttpException("errore nel mettere i permessi a utente con cf " + entitaDaModificare.getCodiceFiscale(), ex);
                        }
                    }
                }
            }
            default ->
                throw new AssertionError();
        }
    }

    public void menageContattoAppartenenteUnificato(RepositoryFactory repositoryFactory) {
        for (UtenteStruttura utenteStrutturaNew : utenteStrutturaDaInserireList) {
            log.info("sto gestendo utente con cf: " + utenteStrutturaNew.getIdUtente().getIdPersona().getCodiceFiscale() + " su struttura " + utenteStrutturaNew.getIdStruttura().getNome()
                + " su azienda " + utenteStrutturaNew.getIdStruttura().getIdAzienda().getId());
            repositoryFactory.getEntityManager().refresh(utenteStrutturaNew);
            Contatto contattoDB = repositoryFactory.getEntityManager().find(Contatto.class, utenteStrutturaNew.getIdUtente().getIdPersona().getIdContatto().getId());
            repositoryFactory.getEntityManager().refresh(contattoDB);
            List<DettaglioContatto> dettaglioContattoList = contattoDB.getDettaglioContattoList();
            repositoryFactory.getEntityManager().refresh(utenteStrutturaNew.getIdStruttura());
            List<DettaglioContatto> dettagliContattiDellaPersona = dettaglioContattoList.stream().filter(dc -> dc.getIdContattoEsterno() != null && dc.getIdContattoEsterno().getId().equals(utenteStrutturaNew.getIdStruttura().getIdContatto().getId())).toList();
            DettaglioContatto idDettaglioContatto = null;
            if (dettagliContattiDellaPersona != null && !dettagliContattiDellaPersona.isEmpty() && dettagliContattiDellaPersona.size() == 1) {
                idDettaglioContatto = dettagliContattiDellaPersona.get(0);
            }
            if (idDettaglioContatto != null) {
                String descrizione = utenteStrutturaNew.getIdStruttura().getNome();
                if (utenteStrutturaNew.getIdStruttura().getIdCasella() != null) {
                    descrizione = descrizione + " [" + utenteStrutturaNew.getIdStruttura().getIdCasella().toString() + "]";
                }
                descrizione = descrizione + " [" + utenteStrutturaNew.getIdStruttura().getIdAzienda().getNome() + "]";
                idDettaglioContatto.setDescrizione(descrizione);
                idDettaglioContatto.setPrincipale(utenteStrutturaNew.getIdAfferenzaStruttura().getCodice().equals(AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA));
                idDettaglioContatto.setEliminato(false);
//                for (DettaglioContatto dettaglioContatto : idDettaglioContatto.getIdContatto().getDettaglioContattoList()) {
//                    dettaglioContatto.setPrincipale(dettaglioContatto.getUtenteStruttura().getIdAfferenzaStruttura().getCodice().equals(AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA));
//                    getEntityManager().persist(dettaglioContatto);
//                }
                getEntityManager().persist(idDettaglioContatto);
            } else {
                //devo creare il dettaglio contatto
                idDettaglioContatto = utenteStrutturaNew.buildDettaglioContatto();
                getEntityManager().persist(idDettaglioContatto);
                utenteStrutturaNew.setIdDettaglioContatto(idDettaglioContatto);
                getEntityManager().persist(utenteStrutturaNew);

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
