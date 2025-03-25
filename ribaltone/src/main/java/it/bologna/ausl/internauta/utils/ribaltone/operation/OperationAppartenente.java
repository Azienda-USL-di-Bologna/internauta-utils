package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.blackbox.exceptions.BlackBoxPermissionException;
import it.bologna.ausl.blackbox.utils.BlackBoxConstants;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.AfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QAfferenzaStruttura;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.QUtenteStruttura;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.baborg.UtenteStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 *
 * @author Top
 */
public class OperationAppartenente extends Operation<DatiRibaltoneInterface> implements Serializable {

    
    private final QPersona qPersona = QPersona.persona;
    private final QUtente qUtente = QUtente.utente;
    private final QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;
    private final QStruttura qStruttura = QStruttura.struttura;
    private final QAfferenzaStruttura qffAfferenzaStruttura = QAfferenzaStruttura.afferenzaStruttura;

    public OperationAppartenente(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager) {
        super(azione, entitaCoinvolta, entityManager);

    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        JPAQueryFactory queryFactory = new JPAQueryFactory(getEntityManager());
        PermissionManager permissionManager = repositoryFactory.getPermissionManager();
        Utente utente = null;
        Struttura struttura = null;
        Persona persona = null;
        UtenteStruttura utenteStruttura = null;
        switch (getAzione()) {

            case INSERT: {
                DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                //faccio fetchFirst perche mi aspetto di trovare una sola persona con quel codice fiscale o di non trovarne affatto
                utenteStruttura = new UtenteStruttura();
                struttura = getStruttura(queryFactory, entitaDaInserire.getIdCasella(), entitaDaInserire.getIdAzienda());
                persona = getPersona(queryFactory, entitaDaInserire);
                if (struttura != null) {
                    if (persona == null) {
                        //faccio fetchFirst perche mi aspetto di trovare un solo utente per azienda o di non trovarne affatto
                        persona = new Persona();
                        persona.setNome(entitaDaInserire.getNome());
                        persona.setCognome(entitaDaInserire.getCognome());
                        persona.setCodiceFiscale(entitaDaInserire.getCodiceFiscale());
                        persona.setDescrizione(entitaDaInserire.getCognome() + " " + entitaDaInserire.getNome());
                        persona.setIdAziendaDefault(struttura.getIdAzienda());
                    }
                    persona.setAttiva(Boolean.TRUE);
                    utente = getUtente(queryFactory, entitaDaInserire, persona);

                    if (utente == null) {
                        utente = new Utente();
                        utente.setIdAzienda(struttura.getIdAzienda());
                        utente.setUsername(entitaDaInserire.getUsername());
                        utente.setOmonimia(Boolean.FALSE);
                    }
                    utente.setAttivo(Boolean.TRUE);
                    utente.setDataSpegnimento(null);
                    utente.setIdPersona(persona);

                    utenteStruttura.setIdUtente(utente);
                    utenteStruttura.setAttivoDal(ZonedDateTime.now());
                    utenteStruttura.setIdStruttura(struttura);
                    utenteStruttura.setAttivo(Boolean.TRUE);
                    utenteStruttura.setIdAfferenzaStruttura(getAfferenzaFromSigla(queryFactory, entitaDaInserire.getTipoAppartenenza()));
                    utenteStruttura.setResponsabile(entitaDaInserire.getResposabile());

                } else {
                    throw new RibaltoneHttpException(" non trovata la struttura di un utente qualcosa nei controlli è andato male!!!");
                }
                //inserire in baborg persone se non c'è la persona
                //inserire in baborg utenti se non c'è l'utente
                //inserire in baborg utenti struttura se non c'è l'afferenza ricordandosi di una sola afferenza diretta e n funzionali
                //inserire i permessi di flusso per i responsabili
                getEntityManager().persist(utenteStruttura);
                if (entitaDaInserire.getResposabile()) {
                    try {
                        permissionManager.insertSimplePermission(
                                utente,
                                struttura,
                                BlackBoxConstants.Predicato.FIRMA.toString(),
                                "ribaltone",
                                Boolean.FALSE,
                                Boolean.FALSE,
                                BlackBoxConstants.Ambito.PICO.toString(),
                                BlackBoxConstants.Tipo.FLUSSO.toString());
                    } catch (BlackBoxPermissionException ex) {
                        throw new RibaltoneHttpException("errore nella creazione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                    }
                }
            //TODO: ora gestisco il caso in cui inserisco un utente unificato
            }
            break;

            case CHIUSURA: {
                DatiImportatiAppartenente entitaDaChiudere = (DatiImportatiAppartenente) getEntitaCoinvolta();
                persona = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaChiudere.getCodiceFiscale()).and(qPersona.attiva)).fetchFirst();
                if (persona != null) {
                    utente = getUtente(queryFactory, entitaDaChiudere, persona);
                }
                struttura = getStruttura(queryFactory, entitaDaChiudere.getIdCasella(), entitaDaChiudere.getIdAzienda());
                //chiudere in baborg utenti struttura
                //chiudere in baborg utenti se non ci sono afferenze attive
                //chiudere in baborg persone se non ci sono utenti attivi
                if (utente != null && struttura != null) {
                    utenteStruttura = getUtenteStruttura(queryFactory, struttura, utente);
                    utenteStruttura.setAttivo(Boolean.FALSE);
                    utenteStruttura.setAttivoAl(ZonedDateTime.now());
                    //spengni tutti i permessi veicolati
                    try {
                                permissionManager.deletePermission(
                                        persona,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "ribaltone",
                                        struttura);
                    //spengo anche questi anche se ad oggi non abbiamo permessi veicolati sugli utenti
                                permissionManager.deletePermission(
                                        utente,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "ribaltone",
                                        struttura);
                            } catch (BlackBoxPermissionException ex) {
                                throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                            }
                    if (!utenteHasOtherStrutture(queryFactory, struttura, utente)) {
                        utente.setAttivo(false);
                        utente.setDataSpegnimento(ZonedDateTime.now());
                        //spegnere tutti i permessi utente 
                        try {
                                permissionManager.deletePermission(
                                        utente,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        "ribaltone");
                            } catch (BlackBoxPermissionException ex) {
                                throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                            }
                        if (!personaHasOtherUtenti(queryFactory, persona)) {
                            persona.setAttiva(Boolean.FALSE);
                            persona.setDataSpegnimento(ZonedDateTime.now());
                            utente.setIdPersona(persona);
                            //spegni tutti i permessi persona
                            try {
                                permissionManager.deletePermission(
                                        persona,
                                        null,
                                        null,
                                        null,
                                       null,
                                        null,
                                        null,
                                        null,
                                        "ribaltone");
                            } catch (BlackBoxPermissionException ex) {
                                throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                            }
                        }
                        utenteStruttura.setIdUtente(utente);
                    }
                    getEntityManager().persist(utenteStruttura);
                }

                //chiudere tutti i permessi di flusso e veicolati per la struttura di riferimento
                if (entitaDaChiudere.getResposabile()) {
                    try {
                         permissionManager.deletePermission(
                                        utente,
                                        struttura,
                                        BlackBoxConstants.Predicato.FIRMA.toString(),
                                        "ribaltone",
                                        Boolean.FALSE,
                                        Boolean.FALSE,
                                        BlackBoxConstants.Ambito.PICO.toString(),
                                        BlackBoxConstants.Tipo.FLUSSO.toString(),
                                        "ribaltone");
                    } catch (BlackBoxPermissionException ex) {
                        throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                    }
                }
                //TODO: ora gestisco il caso in cui inserisco un utente unificato
            }
            break;
            case EDIT: {
                //sicuramente non ha cambiato struttura perche questa operazione si traduce in una insert e una chiusura
                DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                persona = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale())).fetchFirst();
                // è il caso di utente che diventa o non è più responsabile,
                // quindi verificare i permessi di flusso
                struttura = getStruttura(queryFactory, entitaDaInserire.getIdCasella(), entitaDaInserire.getIdAzienda());
                if (persona != null) {
                    utente = getUtente(queryFactory, entitaDaInserire, persona);
                    persona.setCognome(entitaDaInserire.getCognome());
                    persona.setNome(entitaDaInserire.getNome());
                    persona.setDescrizione(entitaDaInserire.getCognome() + " " + entitaDaInserire.getNome());

                }
                if (utente != null && struttura != null) {
                    utente.setUsername(entitaDaInserire.getUsername());
                    utente.setIdPersona(persona);
                    utenteStruttura = getUtenteStruttura(queryFactory, struttura, utente);
                    if (utenteStruttura != null) {
                        utenteStruttura.setResponsabile(entitaDaInserire.getResposabile());
                        utenteStruttura.setIdUtente(utente);
                        utenteStruttura.setIdAfferenzaStruttura(getAfferenzaFromSigla(queryFactory, entitaDaInserire.getTipoAppartenenza()));
                        getEntityManager().persist(utenteStruttura);
                        if (entitaDaInserire.getResposabile()) {
                            try {
                                permissionManager.insertSimplePermission(
                                        utente,
                                        struttura,
                                        BlackBoxConstants.Predicato.FIRMA.toString(),
                                        "ribaltone",
                                        Boolean.FALSE,
                                        Boolean.FALSE,
                                        BlackBoxConstants.Ambito.PICO.toString(),
                                        BlackBoxConstants.Tipo.FLUSSO.toString());
                            } catch (BlackBoxPermissionException ex) {
                                throw new RibaltoneHttpException("errore nella creazione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                            }
                        } else {
                            try {
                                permissionManager.deletePermission(
                                        utente,
                                        struttura,
                                        BlackBoxConstants.Predicato.FIRMA.toString(),
                                        "ribaltone",
                                        Boolean.FALSE,
                                        Boolean.FALSE,
                                        BlackBoxConstants.Ambito.PICO.toString(),
                                        BlackBoxConstants.Tipo.FLUSSO.toString(),
                                        "ribaltone");
                            } catch (BlackBoxPermissionException ex) {
                                throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                            }
                        }
                    }
                }
                //che cambia nome
                // verificare baborg.persona
                //che cambia tipologia verificare baborg.perdi afferenza
                //verificare baborg.utenti_strutttura
                //che cambia username
                //modificare username su baborg.utenti
                //TODO: ora gestisco il caso in cui edito un utente unificato
                break;
            }
        }
    }

    private Struttura getStruttura(JPAQueryFactory queryFactory, Integer idCasella, Integer idAzienda) {
        return queryFactory
                .select(qStruttura)
                .from(qStruttura)
                .where(qStruttura.idCasella.eq(idCasella)
                        .and(qStruttura.attiva)
                        .and(qStruttura.idAzienda.id.eq(idAzienda)))
                .fetchFirst();
    }

    private Utente getUtente(JPAQueryFactory queryFactory, DatiRibaltoneInterface entitaDaInserire, Persona persona) {
        if (persona.getId() != null) {
            return queryFactory
                    .select(qUtente)
                    .from(qUtente)
                    .where(qUtente.idPersona.id.eq(persona.getId())
                            .and(qUtente.idAzienda.id.eq(entitaDaInserire.getIdAzienda())))
                    .fetchFirst();
        } else {
            return null;
        }
    }

    private UtenteStruttura getUtenteStruttura(JPAQueryFactory queryFactory, Struttura struttura, Utente utente) {
        if (struttura != null && utente != null) {
            return queryFactory
                    .select(qUtenteStruttura)
                    .from(qUtenteStruttura)
                    .where(qUtenteStruttura.idUtente.id.eq(utente.getId())
                            .and(qUtenteStruttura.idStruttura.id.eq(struttura.getId())).and(qUtenteStruttura.attivo))
                    .fetchFirst();
        } else {
            return null;
        }
    }

    private Boolean utenteHasOtherStrutture(JPAQueryFactory queryFactory, Struttura struttura, Utente utente) {
        if (struttura != null && utente != null) {
            return queryFactory
                    .select(qUtenteStruttura.id)
                    .from(qUtenteStruttura)
                    .where(qUtenteStruttura.idUtente.id.eq(utente.getId())
                            .and(qUtenteStruttura.attivo)).limit(1).fetchFirst() != null;
        } else {
            return null;
        }
    }

    private Persona getPersona(JPAQueryFactory queryFactory, DatiDaImportareAppartenente entitaDaInserire) {
        return queryFactory
                .select(qPersona)
                .from(qPersona)
                .where(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale()))
                .fetchFirst();
    }

    private Boolean personaHasOtherUtenti(JPAQueryFactory queryFactory, Persona persona) {
        if (persona != null) {
            return queryFactory
                    .select(qUtente.id)
                    .from(qUtente)
                    .where(qUtente.idPersona.id.eq(persona.getId()).and(qUtente.attivo))
                    .fetchFirst() != null;
        } else {
            return null;
        }
    }

    private AfferenzaStruttura getAfferenzaFromSigla(JPAQueryFactory queryFactory, String sigla) {
        //
        AfferenzaStruttura.CodiciAfferenzaStruttura codice;
        switch (sigla) {
            case "F":
            case "f":
                codice = AfferenzaStruttura.CodiciAfferenzaStruttura.FUNZIONALE;
                break;
            case "D":
            case "d":
                codice = AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA;
                break;
            default:
                return null;
        }
        return queryFactory
                .select(qffAfferenzaStruttura)
                .from(qffAfferenzaStruttura)
                .where(qffAfferenzaStruttura.codice.eq(codice.toString()))
                .fetchFirst();

    }
}
