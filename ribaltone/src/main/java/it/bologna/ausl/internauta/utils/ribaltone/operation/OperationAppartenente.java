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

    private String nomeCasella;
    private List<String> listOfEdit;

    public OperationAppartenente(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager, List<String> listOfEdit, String nomeCasella) {
        super(azione, entitaCoinvolta, entityManager);
        this.listOfEdit = listOfEdit;
        this.nomeCasella = nomeCasella;
    }

    public OperationAppartenente(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager, List<String> listOfEdit) {
        super(azione, entitaCoinvolta, entityManager);
        this.listOfEdit = listOfEdit;
    }

    public List<String> getListOfEdit() {
        return listOfEdit;
    }

    public void setListOfEdit(List<String> listOfEdit) {
        this.listOfEdit = listOfEdit;
    }

    public String getNomeCasella() {
        return nomeCasella;
    }

    public void setNomeCasella(String nomeCasella) {
        this.nomeCasella = nomeCasella;
    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
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
                                utenteStruttura.setResponsabile(entitaDaInserire.getResponsabile());
                                repositoryFactory.getEntityManager().persist(utenteStruttura);
                                if (entitaDaInserire.getResponsabile()) {
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
                            repositoryFactory.getEntityManager().persist(utenteStruttura);
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
                                    utenteStruttura.setResponsabile(entitaDaInserire.getResponsabile());
                                    utenteStruttura.setIdUtente(utente);
                                    utenteStruttura.setIdAfferenzaStruttura(getAfferenzaFromSigla(queryFactory, entitaDaInserire.getTipoAppartenenza()));
                                    repositoryFactory.getEntityManager().persist(utenteStruttura);
                                    if (entitaDaInserire.getResponsabile()) {
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
            List<Utente> utentiRibaltonici = ribaltone.getUtenteList().stream().filter(u -> u.getIdAzienda().equals(getEntitaCoinvolta().getIdAzienda())).toList();
            Utente ribaltoneUser;
            if (utentiRibaltonici != null) {
                ribaltoneUser = utentiRibaltonici.get(0);

                switch (getAzione()) {
                    //va verificato il perche se perche confluito allora va gestito il caso
                    case INSERT -> {
                        DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                        Struttura strutturaAttiva = queryFactory.select(qStruttura).from(qStruttura).where(qStruttura.attiva.and(qStruttura.idCasella.eq(entitaDaInserire.getIdCasella()))).fetchOne();
                        List<DatiDaImportareTrasformazione> trasformazioniInerenti = datiDaImportareTrasformazioneList.stream().filter(t -> t.getIdCasellaArrivo().equals(entitaDaInserire.getIdCasella())).toList();
                        //se non ci sono trasformazioni inerenti è davvero un nuovo utente

                        Persona p = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale())).fetchOne();
                        if (p != null) {
                            Contatto c = p.getIdContatto();
                            if (c == null) {
                                Integer[] idAziende = new Integer[1];
                                idAziende[0] = entitaDaInserire.getIdAzienda();
                                c = p.buildContatto(idAziende, ribaltone, ribaltoneUser);
                            }
                            List<Utente> utenti = p.getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(entitaDaInserire.getIdAzienda())).toList();
                            if (!utenti.isEmpty()) {
                                Utente utente = utenti.get(0);
                                if (trasformazioniInerenti.isEmpty()) {
                                    List<UtenteStruttura> usList = utente.getUtenteStrutturaList().stream().filter(us -> us.getIdStruttura().getId().equals(strutturaAttiva.getId())).toList();
                                    if (!usList.isEmpty()) {
                                        UtenteStruttura utenteStruttura = usList.get(0);
                                        DettaglioContatto dc = new DettaglioContatto();
                                        dc.setIdContatto(c);
                                        dc.setDescrizione(strutturaAttiva.getNome() + " [" + strutturaAttiva.getIdCasella().toString() + "] [" + strutturaAttiva.getIdAzienda().getNome() + "]");
                                        dc.setUtenteStruttura(utenteStruttura);
                                        dc.setPrincipale(utenteStruttura.getIdAfferenzaStruttura().getCodice().equals(CodiciAfferenzaStruttura.DIRETTA));
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
                            if (usVecchio != null) {
                                DettaglioContatto idDettaglioContatto = usVecchio.getIdDettaglioContatto();
                                idDettaglioContatto.setEliminato(Boolean.TRUE);
                                entityManager.persist(idDettaglioContatto);
                                for (GruppiContatti gc : idDettaglioContatto.getGruppiDelDettaglioList()) {
                                    gc.setEliminato(Boolean.TRUE);
                                    gc.setEliminatoDa("ribaltone");
                                    entityManager.persist(gc);
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
                                        dc -> dc.getIdContattoEsterno() != null && dc.getIdContattoEsterno().getIdStruttura().getIdCasella().equals(entitaDaInserire.getIdCasella())
                                    ).toList();
                                    if (!dcList.isEmpty()) {
                                        DettaglioContatto dc = dcList.get(0);
                                        dc.setPrincipale(entitaDaInserire.getTipoAppartenenza().equalsIgnoreCase("T"));
                                        entityManager.persist(dc);
                                    }
                                }
                                default ->
                                    throw new AssertionError();
                            }
                            entityManager.persist(idContatto);
                        }
                    }
                }
            }
        }
    }
}
