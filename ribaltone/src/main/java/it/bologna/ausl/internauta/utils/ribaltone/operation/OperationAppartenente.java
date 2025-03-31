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
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        //Utente utente = null;
        List<Utente> utenti = new ArrayList<>();
        Struttura strutturaAppartenteOriginale = null;
        Persona persona = null;
        UtenteStruttura utenteStruttura = null;
        List<Integer> idAziendeList = new ArrayList<>();
        List<Struttura> struttureUnificate = new ArrayList<>();
        List<StrutturaUnificata> entitaCoinvoltaUnificazioni = OperationsUtils.entitaCoinvoltaTouchUnificazioni(queryFactory, getEntitaCoinvolta(), qStruttura);
        if (entitaCoinvoltaUnificazioni != null && !entitaCoinvoltaUnificazioni.isEmpty()) {
            idAziendeList = entitaCoinvoltaUnificazioni.stream().flatMap(a -> Stream.of(
                    a.getIdStrutturaDestinazione().getIdAzienda().getId(),
                    a.getIdStrutturaSorgente().getIdAzienda().getId()
            ))
                    .distinct()
                    .collect(Collectors.toList());
            struttureUnificate = entitaCoinvoltaUnificazioni.stream()
                    .flatMap(a -> Stream.of(
                    a.getIdStrutturaDestinazione(),
                    a.getIdStrutturaSorgente()
            ))
                    .collect(Collectors.toMap(
                            struttura -> struttura.getIdAzienda().getId(), // Usa l'ID Azienda come chiave
                            struttura -> struttura, // Mantieni l'oggetto Struttura
                            (existing, replacement) -> existing // In caso di duplicati, mantiene il primo
                    )).values().stream().toList();
        } else {
            idAziendeList.add(getEntitaCoinvolta().getIdAzienda());
        }

        switch (getAzione()) {
            case INSERT -> {
                DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                //faccio fetchFirst perche mi aspetto di trovare una sola persona con quel codice fiscale o di non trovarne affatto
                utenteStruttura = new UtenteStruttura();
                strutturaAppartenteOriginale = OperationsUtils.getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, entitaDaInserire.getIdCasella(), entitaDaInserire.getIdAzienda(), qStruttura);
                if (entitaCoinvoltaUnificazioni == null || entitaCoinvoltaUnificazioni.isEmpty()) {
                    struttureUnificate.add(strutturaAppartenteOriginale);
                }
                persona = getPersona(queryFactory, entitaDaInserire);
                //inserire in baborg persone se non c'è la persona
                if (strutturaAppartenteOriginale != null) {
                    if (persona == null) {
                        persona = new Persona();
                    }
                    persona.setAttiva(Boolean.TRUE);
                    persona.setNome(entitaDaInserire.getNome());
                    persona.setCognome(entitaDaInserire.getCognome());
                    persona.setCodiceFiscale(entitaDaInserire.getCodiceFiscale());
                    persona.setDescrizione(entitaDaInserire.getCognome() + " " + entitaDaInserire.getNome());
                    persona.setIdAziendaDefault(strutturaAppartenteOriginale.getIdAzienda());

                    utenti = getUtenti(queryFactory, idAziendeList, persona);
                    if (utenti.isEmpty() || utenti.size() != idAziendeList.size()) {
                        //inserire in baborg utenti se non c'è l'utente dell'azienda che lancia il ribaltone
                        //nel caso si stia trattando una struttura unificata allora controllo che si sia 
                        //e nel caso inserisco in quelle aziende l'utente nuovo
                        for (Integer idAzienda : idAziendeList) {
                            Utente utente = new Utente();
                            utente.setIdAzienda(new Azienda(idAzienda));
                            utenti.add(utente);
                        }
                    }
                    //ciclo su tutti gli utenti delle strutture delle aziende unificate
                    for (Utente utente : utenti) {
                        for (Struttura struttura : struttureUnificate) {
                            //se l'utente è quello della struttura che sto considerando faccio cose altrimenti no
                            if (struttura.getIdAzienda().getId().equals(utente.getIdAzienda().getId())) {
                                utente.setUsername(entitaDaInserire.getUsername());
                                utente.setOmonimia(Boolean.FALSE);
                                utente.setAttivo(Boolean.TRUE);
                                utente.setDataSpegnimento(null);
                                utente.setIdPersona(persona);

                                utenteStruttura.setIdUtente(utente);
                                utenteStruttura.setAttivoDal(ZonedDateTime.now());
                                utenteStruttura.setIdStruttura(struttura);
                                utenteStruttura.setAttivo(Boolean.TRUE);
                                //inserire in baborg utenti_struttura se non c'è l'afferenza ricordandosi di una sola afferenza diretta e n funzionali
                                utenteStruttura.setIdAfferenzaStruttura(getAfferenzaFromSigla(queryFactory, struttura.getId().equals(strutturaAppartenteOriginale.getId()) ? entitaDaInserire.getTipoAppartenenza() : "U"));
                                utenteStruttura.setResponsabile(entitaDaInserire.getResposabile());
                                getEntityManager().persist(utenteStruttura);
                                if (entitaDaInserire.getResposabile()) {
                                    try {
                                        permissionManager.insertSimplePermission(
                                                utente,
                                                strutturaAppartenteOriginale,
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
                            }
                        }

                    }
                } else {
                    throw new RibaltoneHttpException(" non trovata la struttura di un utente qualcosa nei controlli è andato male!!!");
                }

                //inserire i permessi di flusso per i responsabili
                //TODO: ora gestisco il caso in cui inserisco un utente unificato
            }

            case CHIUSURA -> {
                DatiImportatiAppartenente entitaDaChiudere = (DatiImportatiAppartenente) getEntitaCoinvolta();
                persona = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaChiudere.getCodiceFiscale()).and(qPersona.attiva)).fetchFirst();
                if (persona != null) {
                    utenti = getUtenti(queryFactory, idAziendeList, persona);
                    strutturaAppartenteOriginale = OperationsUtils.getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, entitaDaChiudere.getIdCasella(), entitaDaChiudere.getIdAzienda(), qStruttura);
                    //chiudere in baborg utenti struttura
                    //chiudere in baborg utenti se non ci sono afferenze attive
                    //chiudere in baborg persone se non ci sono utenti attivi
                    for (Utente utente : utenti) {
                        for (Struttura struttura : struttureUnificate) {
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
                            if (!utenteHasOtherStrutture(queryFactory, strutturaAppartenteOriginale, utente)) {
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
                            //chiudere tutti i permessi di flusso e veicolati per la struttura di riferimento
                            try {
                                permissionManager.deletePermission(
                                        utente,
                                        struttura,
                                        null,
                                        "ribaltone",
                                        Boolean.FALSE,
                                        Boolean.FALSE,
                                        null,
                                        null);
                            } catch (BlackBoxPermissionException ex) {
                                throw new RibaltoneHttpException("errore nella rimozione del permesso per il responsabile " + persona.getDescrizione() + " " + persona.getCodiceFiscale(), ex);
                            }

                        }
                    }
                }
            }
            case EDIT -> {
                //sicuramente non ha cambiato struttura perche questa operazione si traduce in una insert e una chiusura
                // quindi o cambia nome e bisogna verificare baborg.persona
                // o  cambia tipologia afferenza verificare baborg.perdi 
                //verificare baborg.utenti_strutttura
                //che cambia username
                //modificare username su baborg.utenti
                DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                persona = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale())).fetchFirst();
                // è il caso di utente che diventa o non è più responsabile,
                // quindi verificare i permessi di flusso
                strutturaAppartenteOriginale = OperationsUtils.getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, entitaDaInserire.getIdCasella(), entitaDaInserire.getIdAzienda(), qStruttura);
                if (persona != null) {
                    utenti = getUtenti(queryFactory, idAziendeList, persona);
                    persona.setCognome(entitaDaInserire.getCognome());
                    persona.setNome(entitaDaInserire.getNome());
                    persona.setDescrizione(entitaDaInserire.getCognome() + " " + entitaDaInserire.getNome());

                    for (Utente utente : utenti) {
                        for (Struttura struttura : struttureUnificate) {
                            if (utente != null && strutturaAppartenteOriginale != null) {
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
                        }
                    }
                }             
            }
        }
    }

    private List<Utente> getUtenti(JPAQueryFactory queryFactory, List<Integer> idAziende, Persona persona) {
        if (persona.getId() != null) {
            return queryFactory
                    .select(qUtente)
                    .from(qUtente)
                    .where(qUtente.idPersona.id.eq(persona.getId())
                            .and(qUtente.idAzienda.id.in(idAziende)))
                    .fetch();
        } else {
            return new ArrayList<>();
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
        //faccio fetchFirst perche mi aspetto di trovare un solo utente per azienda o di non trovarne affatto
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
        AfferenzaStruttura.CodiciAfferenzaStruttura codice;
        switch (sigla) {
            case "F", "f" ->
                codice = AfferenzaStruttura.CodiciAfferenzaStruttura.FUNZIONALE;
            case "D", "d" ->
                codice = AfferenzaStruttura.CodiciAfferenzaStruttura.DIRETTA;
            case "U" -> {
                codice = AfferenzaStruttura.CodiciAfferenzaStruttura.UNIFICATA;
                break;
            }
            default -> {
                return null;
            }
        }
        return queryFactory
                .select(qffAfferenzaStruttura)
                .from(qffAfferenzaStruttura)
                .where(qffAfferenzaStruttura.codice.eq(codice.toString()))
                .fetchFirst();

    }

//    private Struttura entitaCoinvoltaTouchUnificazione(JPAQueryFactory queryFactory, DatiRibaltoneInterface entitaCoinvolta) {
//        Struttura strutturaCoinvolta = null;
//        switch (entitaCoinvolta.getTipo()) {
//            case "Appartenente":
//                if (entitaCoinvolta.getClasse().equals(DatiDaImportareAppartenente.class.getCanonicalName())) {
//                    DatiDaImportareAppartenente datiDaImportareAppartenente = (DatiDaImportareAppartenente) entitaCoinvolta;
//                    strutturaCoinvolta = getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, datiDaImportareAppartenente.getIdCasella(), datiDaImportareAppartenente.getIdAzienda());
//                } else {
//                    DatiImportatiAppartenente datiImportatiAppartenente = (DatiImportatiAppartenente) entitaCoinvolta;
//                    strutturaCoinvolta = getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, datiImportatiAppartenente.getIdCasella(), datiImportatiAppartenente.getIdAzienda());
//                }
//                break;
//            case "Struttura":
//                if (entitaCoinvolta.getClasse().equals(DatiDaImportareStruttura.class.getCanonicalName())) {
//                    DatiDaImportareStruttura datiDaImportareStruttura = (DatiDaImportareStruttura) entitaCoinvolta;
//                    strutturaCoinvolta = getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, datiDaImportareStruttura.getIdCasella(), datiDaImportareStruttura.getIdAzienda());
//                } else {
//                    DatiImportatiStruttura datiImportatiStruttura = (DatiImportatiStruttura) entitaCoinvolta;
//                    strutturaCoinvolta = getStrutturaFromIdCasellaAndIdAziendaAndAttiva(queryFactory, datiImportatiStruttura.getIdCasella(), datiImportatiStruttura.getIdAzienda());
//                }
//                break;
//
//            default:
//                throw new AssertionError();
//        }
//        return strutturaCoinvolta;
//    }
}
