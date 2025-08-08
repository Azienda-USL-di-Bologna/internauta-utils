package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.CHIUSURA;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.EDIT;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.INSERT;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura.CodiciAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.StrutturaUnificata;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.GruppiContatti;
import it.bologna.ausl.model.entities.rubrica.QDettaglioContatto;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 */
public class OperationAppartenente extends Operation<DatiRibaltoneInterface> implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(OperationAppartenente.class);
    private final QPersona qPersona = QPersona.persona;
    private final QUtente qUtente = QUtente.utente;
    private final QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;
    private final QStruttura qStruttura = QStruttura.struttura;
    private final QAfferenzaStruttura qffAfferenzaStruttura = QAfferenzaStruttura.afferenzaStruttura;

    private List<String> listOfEdit;

    public OperationAppartenente(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager, List<String> listOfEdit, Map<String, String> descrizioniAggiuntive) {
        super(azione, entitaCoinvolta, entityManager, descrizioniAggiuntive);
        this.listOfEdit = listOfEdit;
    }

//    public OperationAppartenente(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager, List<String> listOfEdit) {
//        super(azione, entitaCoinvolta, entityManager);
//        this.listOfEdit = listOfEdit;
//    }
    public List<String> getListOfEdit() {
        return listOfEdit;
    }

    public void setListOfEdit(List<String> listOfEdit) {
        this.listOfEdit = listOfEdit;
    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        PermissionManager permissionManager = repositoryFactory.getPermissionManager();
        //Utente utente = null;
        Utente utente;
        Struttura strutturaAppartenteOriginale = null;
        Persona persona = null;
        UtenteStruttura utenteStruttura = null;
        switch (getAzione()) {
            case INSERT -> {
                DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                Struttura struttura = OperationsUtils.getStrutturaAttivaFromIdCasellaAndIdAzienda(queryFactory, entitaDaInserire.getIdCasella(), entitaDaInserire.getIdAzienda(), qStruttura);
                OperationsUtils.insertUtenteInStruttura(queryFactory, entitaDaInserire, struttura, repositoryFactory.getEntityManager(), permissionManager, null);
            }
            case CHIUSURA -> {
                DatiImportatiAppartenente entitaDaChiudere = (DatiImportatiAppartenente) getEntitaCoinvolta();

                OperationsUtils.chiudiUtenteStruttura(entitaDaChiudere, entitaDaChiudere.getIdCasella(), entitaDaChiudere.getIdAzienda(), queryFactory, permissionManager, getEntityManager(), null);

            }
            case EDIT -> {
                //sicuramente non ha cambiato struttura perche questa operazione si traduce in una insert e una chiusura
                // quindi o cambia nome e bisogna verificare baborg.persona
                // o  cambia tipologia afferenza verificare baborg.perdi
                //verificare baborg.utenti_strutttura
                //che cambia username
                //modificare username su baborg.utenti
                DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();

                // è il caso di utente che diventa o non è più responsabile,
                // quindi verificare i permessi di flusso
                strutturaAppartenteOriginale = OperationsUtils.getStrutturaAttivaFromIdCasellaAndIdAzienda(queryFactory, entitaDaInserire.getIdCasella(), entitaDaInserire.getIdAzienda(), qStruttura);
                OperationsUtils.editUtenteStruttura(strutturaAppartenteOriginale, entitaDaInserire, queryFactory, getEntityManager(), permissionManager, null);
            }
        }
    }

//    private Boolean personaHasOtherUtenti(JPAQueryFactory queryFactory, Persona persona) {
//        if (persona != null) {
//            return queryFactory
//                .select(qUtente.id)
//                .from(qUtente)
//                .where(qUtente.idPersona.id.eq(persona.getId()).and(qUtente.attivo))
//                .fetchFirst() != null;
//        } else {
//            return null;
//        }
//    }
    /**
     * in questa funzione mi occupero solo dell'inserimento e della rimozione
     * dei contatti di tipo utente struttura che NON riguardano le
     * trasformazioni quelli li tocco solo nelle trasformazioni
     *
     * @param repositoryFactory
     * @throws RibaltoneHttpException
     */
    public void menageContattoAppartenente(RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        EntityManager entityManager = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QDatiDaImportareStruttura qDatiDaImportareStruttura = QDatiDaImportareStruttura.datiDaImportareStruttura;
        QDatiDaImportareTrasformazione qDatiDaImportareTrasformazione = QDatiDaImportareTrasformazione.datiDaImportareTrasformazione;
        QDettaglioContatto qDettaglioContatto = QDettaglioContatto.dettaglioContatto;
        Integer idAzienda = getEntitaCoinvolta().getIdAzienda();
        List<DatiDaImportareStruttura> datiDaImportareStrutturaList = queryFactory.select(qDatiDaImportareStruttura).from(qDatiDaImportareStruttura).where(qDatiDaImportareStruttura.idAzienda.eq(idAzienda)).fetch();
        List<DatiDaImportareTrasformazione> datiDaImportareTrasformazioneList = queryFactory.select(qDatiDaImportareTrasformazione).from(qDatiDaImportareTrasformazione).where(qDatiDaImportareTrasformazione.idAzienda.eq(idAzienda)).fetch();
        //Map<String, Integer> indexStrutture = RibaltoneUtils.generateIndex(datiDaImportareStrutturaList, DatiDaImportareStruttura::getKey);
        //Map<String, Integer> indexTrasformazioni = RibaltoneUtils.generateIndex(datiDaImportareTrasformazioneList, DatiDaImportareTrasformazione::getKey);
        Persona ribaltone = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq("RIBALTONE")).fetchOne();
        if (ribaltone != null) {
            List<Utente> utentiRibaltonici = ribaltone.getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(getEntitaCoinvolta().getIdAzienda())).toList();
            Utente ribaltoneUser;
            if (utentiRibaltonici != null) {
                ribaltoneUser = utentiRibaltonici.get(0);

                switch (getAzione()) {
                    //va verificato il perche se perche confluito allora va gestito il caso
                    case INSERT -> {
                        DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                        Struttura strutturaAttiva = queryFactory.select(qStruttura).from(qStruttura).where(qStruttura.attiva.and(qStruttura.idCasella.eq(entitaDaInserire.getIdCasella()))).fetchOne();
                        List<DatiDaImportareTrasformazione> trasformazioniInerenti = new ArrayList<>();
                        for (DatiDaImportareTrasformazione t : datiDaImportareTrasformazioneList) {
                            try {
                                if (t.getIdCasellaPartenza().equals(entitaDaInserire.getIdCasella()) || (t.getIdCasellaArrivo() != null && t.getIdCasellaArrivo().equals(entitaDaInserire.getIdCasella()))) {
                                    trasformazioniInerenti.add(t);
                                }
                            } catch (Exception e) {
                                log.error("errore qui, " + t.getIdCasellaPartenza() + "  " + t.getIdCasellaArrivo());
                                throw e;
                            }
                        }

                        //se non ci sono trasformazioni inerenti è davvero un nuovo utente
                        Persona p = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale())).fetchOne();
                        if (p != null) {
                            Contatto c = p.getIdContatto();
                            if (c == null) {
                                Integer[] idAziende = new Integer[1];
                                idAziende[0] = entitaDaInserire.getIdAzienda();
                                c = p.buildContatto(idAziende, ribaltone, ribaltoneUser);
                            }
                            log.info("persona " + p.getDescrizione() + " con utenti n ");
                            List<Utente> utentiList = queryFactory.select(qUtente).from(qUtente).where(qUtente.idPersona.id.eq(p.getId())).fetch();
                            utentiList = utentiList.stream().filter(u -> u.getIdAzienda().getId().equals(entitaDaInserire.getIdAzienda())).toList();
                            if (!utentiList.isEmpty()) {
                                Utente utente = repositoryFactory.getEntityManager().find(Utente.class, utentiList.get(0).getId());
                                if (trasformazioniInerenti.isEmpty()) {
//                                    List<UtenteStruttura> usList1 = utente.getUtenteStrutturaList().stream().filter(us -> us.getIdStruttura().getId().equals(strutturaAttiva.getId())).toList();
                                    List<UtenteStruttura> usList = queryFactory.select(qUtenteStruttura).from(qUtenteStruttura).where(qUtenteStruttura.idUtente.id.eq(utente.getId()).and(qUtenteStruttura.idStruttura.id.eq(strutturaAttiva.getId()))).fetch();
                                    if (!usList.isEmpty()) {
                                        UtenteStruttura utenteStruttura = usList.get(0);
                                        DettaglioContatto dc = new DettaglioContatto();

                                        dc.setIdContatto(c);
                                        dc.setDescrizione(strutturaAttiva.getNome() + " [" + strutturaAttiva.getIdCasella().toString() + "] [" + strutturaAttiva.getIdAzienda().getNome() + "]");
                                        if (c.getId() != null) {
                                            DettaglioContatto dcOnDb = queryFactory.select(qDettaglioContatto).from(qDettaglioContatto).where(qDettaglioContatto.idContatto.id.eq(c.getId()).and(qDettaglioContatto.descrizione.eq(dc.getDescrizione()))).fetchOne();
                                            if (dcOnDb != null) {
                                                dc = dcOnDb;
                                            }
                                        }
                                        dc.setUtenteStruttura(utenteStruttura);
                                        dc.setPrincipale(utenteStruttura.getIdAfferenzaStruttura().getCodice().equals(CodiciAfferenzaStruttura.DIRETTA));
                                        dc.setTipo(DettaglioContatto.TipoDettaglio.UTENTE_STRUTTURA);
                                        dc.setEliminato(false);
                                        entityManager.persist(dc);
                                    }
                                    entityManager.refresh(c);
                                    p.setIdContatto(c);
                                    entityManager.persist(p);
                                } else {
//                                    //ci sono trasformazioni quindi sono qui per via di una confluenza
//                                    //nel caso di una rinomina della struttura l'utente non cambia codice casella
//                                    //nel caso di un cambio padre della struttura l'utente non cambia codice casella
//                                    //il dettaglio di tipo UTENTE_STRUTTURA nel caso di confluenza è un nuovo dettaglio e devo cambiarlo anche nei gruppi
//                                    //il dettaglio di tipo UTENTE_STRUTTURA nel caso di rinomina è lo stesso ma col nome nuovo (cosi non devo gestire i gruppi)
//                                    //il dettaglio di tipo UTENTE_STRUTTURA nel caso di cambio padre è lo stesso

//
//                                    for (DatiDaImportareTrasformazione datiDaImportareTrasformazione : trasformazioniInerenti) {
//                                        Integer idCasellaPartenza = datiDaImportareTrasformazione.getIdCasellaPartenza();
//                                        //ultima struttura spenta (quella che ha ancora i contatti vecchi)
//                                        Struttura strutturaVecchia = queryFactory
//                                            .select(qStruttura)
//                                            .from(qStruttura)
//                                            .where(
//                                                qStruttura.idCasella.eq(idCasellaPartenza)
//                                                    .and(qStruttura.attiva.not()))
//                                            .orderBy(qStruttura.dataCessazione.desc()).limit(1).fetchOne();
//                                        if (strutturaVecchia != null) {
//
//                                            if (usVecchio != null) {
//                                                DettaglioContatto idDettaglioContattoVecchio = usVecchio.getIdDettaglioContatto();
//                                                idDettaglioContattoVecchio.setEliminato(Boolean.TRUE);
//                                                List<GruppiContatti> gruppiDelDettaglioList = idDettaglioContattoVecchio.getGruppiDelDettaglioList();
//                                                for (GruppiContatti gruppiContatti : gruppiDelDettaglioList) {
//                                                    gruppiContatti.setEliminatoDa("ribaltone");
//                                                    gruppiContatti.setEliminato(Boolean.TRUE);
//                                                }
//                                            }
//                                        }
//                                    }
                                }
                            }

                        }

                    }
                    //  va verificato il modivo. o chiusa perche utente non piu attivo
                    //  se confluito in questo caso bisogna gestire i gruppi
                    case CHIUSURA -> {
                        DatiImportatiAppartenente entitaDaChiudere = (DatiImportatiAppartenente) getEntitaCoinvolta();

                        List<DatiDaImportareTrasformazione> trasformazioniInerenti = datiDaImportareTrasformazioneList.stream().filter(t -> t.getIdCasellaPartenza().equals(entitaDaChiudere.getIdCasella())).toList();
                        if (trasformazioniInerenti != null && trasformazioniInerenti.isEmpty()) {
                            //chiudo l'afferenza senza trasformazioni quindi posso semplicemente eliminare il dettaglio contatto
                            UtenteStruttura usVecchio = queryFactory.select(qUtenteStruttura).from(qUtenteStruttura).where(
                                qUtenteStruttura.attivo.not().and(
                                    qUtenteStruttura.idStruttura.idCasella.eq(entitaDaChiudere.getIdCasella()).and(
                                        qUtenteStruttura.idUtente.idPersona.codiceFiscale.eq(entitaDaChiudere.getCodiceFiscale())))).orderBy(qUtenteStruttura.attivoAl.desc()).limit(1).fetchOne();
                            if (usVecchio != null && usVecchio.getIdDettaglioContatto() != null) {
                                DettaglioContatto idDettaglioContatto = usVecchio.getIdDettaglioContatto();
                                idDettaglioContatto.setEliminato(Boolean.TRUE);
                                entityManager.persist(idDettaglioContatto);
                                for (GruppiContatti gc : idDettaglioContatto.getGruppiDelDettaglioList()) {
                                    gc.setEliminato(Boolean.TRUE);
                                    gc.setEliminatoDa("ribaltone");
                                    entityManager.persist(gc);
                                }
                                Contatto idContatto = idDettaglioContatto.getIdContatto();
                                if (idContatto.getDettaglioContattoList() == null
                                    || idContatto.getDettaglioContattoList().isEmpty()
                                    || idContatto.getDettaglioContattoList().stream().filter(dc -> dc.getEliminato() == false).toList().isEmpty()) {
                                    idContatto.setEliminato(Boolean.TRUE);
                                    entityManager.persist(idContatto);
                                }
                            }
                        } else {
                            //ci penso nelle trasformazioni
                        }

                    }
                    //sicuramente non ha cambiato struttura perche questa operazione si traduce in una insert e una chiusura
                    //quindi o cambia nome e bisogna verificare baborg.persona
                    // o  cambia tipologia afferenza verificare baborg.utente_struttura
                    //in questo caso il dettaglio principale deve cambiare
                    case EDIT -> {
                        DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                        Persona persona = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale())).fetchFirst();
                        Contatto idContatto = persona.getIdContatto();
                        if (idContatto != null) {

                            log.info("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa " + entitaDaInserire.getCodiceFiscale());
                            for (String edit : listOfEdit) {
                                switch (edit) {
                                    case "cognome" -> {
                                        idContatto.setCognome(entitaDaInserire.getCognome());
                                    }
                                    case "nome" -> {
                                        idContatto.setNome(entitaDaInserire.getNome());
                                    }
                                    case "afferenza" -> {
                                        List<DettaglioContatto> dcList = idContatto.getDettaglioContattoList().stream().filter(
                                            dc -> dc.getIdContattoEsterno() != null
                                            && dc.getIdContattoEsterno().getIdStruttura() != null
                                            && dc.getIdContattoEsterno().getIdStruttura().getIdCasella() != null
                                            && dc.getIdContattoEsterno().getIdStruttura().getIdCasella().equals(entitaDaInserire.getIdCasella())
                                        ).toList();
                                        if (!dcList.isEmpty()) {
                                            DettaglioContatto dc = dcList.get(0);
                                            dc.setPrincipale(entitaDaInserire.getTipoAppartenenza().equalsIgnoreCase("T"));
                                            entityManager.persist(dc);
                                        }
                                    }
                                    case "responsabile" -> {

                                    }
                                    default -> {
                                        log.info(edit);
                                        throw new AssertionError();
                                    }
                                }
                                entityManager.persist(idContatto);
                            }
                        }
                    }
                }
            }
        }
    }

    private List<UtenteStruttura> getUtentiStruttureVeicolati(JPAQueryFactory queryFactory, Struttura strutturaAppartenteOriginale) {
        return queryFactory.select(qUtenteStruttura).from(qUtenteStruttura).where(qUtenteStruttura.idStrutturaVeicolante.id.eq(strutturaAppartenteOriginale.getId()).and(qUtenteStruttura.attivo)).fetch();
    }
}
