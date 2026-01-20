package it.bologna.ausl.internauta.utils.ribaltone.utils;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.common.utils.SimpleMailSenderUtility;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAnagrafica;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationTrasformazione;

import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.AziendaParametriJson;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.QApplicazione;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiTrasformazione;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.DettaglioAttivita;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author gusgus
 */
@Component
public class UsersNotifiesManager {

    private static final Logger log = LoggerFactory.getLogger(UsersNotifiesManager.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SimpleMailSenderUtility simpleMailSenderUtility;

    /*
     * Genera e invia le notifiche/mail agli utenti interessati per le operazioni ribaltone
     * @param buildedOperations le operazioni ribaltone
     * @param codiceAzienda il codice dell'azienda
     * @param idPersonaLanciante l'ID della persona che ha lanciato il ribaltone (null se automatico)
     * @param idPersoneDaNotificare lista di ID persone da notificare (per ribaltone automatico, null se manuale)
     */
    public void generaAndInviaNotifiche(Operations buildedOperations, String codiceAzienda, List<Integer> idPersoneDaNotificare, List<String> mailDaNotificare, String descrizioneErrore) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QAzienda qAzienda = QAzienda.azienda;
        Azienda azienda = queryFactory.select(qAzienda).from(qAzienda).where(qAzienda.codice.eq(codiceAzienda)).fetchOne();

        QApplicazione qApplicazione = QApplicazione.applicazione;
        Applicazione app = queryFactory.select(qApplicazione).from(qApplicazione).where(qApplicazione.id.eq(Applicazione.Applicazioni.ribaltorg.toString())).fetchOne();

        // Ricavare le persone da notificare:
        // in caso di ribaltone manuale è la persona che ha lanciato il ribaltone manuale,
        // in caso di ribaltone automatico sono le persone scritte nella configurazione di ribaltone
        List<Persona> personeDaNotificareSuScrivania = new ArrayList<>();
        //List<Persona> personeDaNotificareTeamiteMail = new ArrayList<>();

        QPersona qPersona = QPersona.persona;

        if (idPersoneDaNotificare != null && !idPersoneDaNotificare.isEmpty() && descrizioneErrore != null) {
            // Ribaltone automatico: notificare le persone dalla configurazione
            List<Persona> persone = queryFactory.select(qPersona).from(qPersona).where(qPersona.id.in(idPersoneDaNotificare)).fetch();
            personeDaNotificareSuScrivania.addAll(persone);
        } else {
            log.info("Nessuna persona da notificare");
        }

        // Genera le attività per ogni persona
        for (Persona p : personeDaNotificareSuScrivania) {
            insertAttivita(azienda, p, app, buildedOperations, queryFactory, descrizioneErrore);
        }

        if (!mailDaNotificare.isEmpty()) {
            sendEmail(mailDaNotificare, azienda, buildedOperations, descrizioneErrore);
        }

    }

    /**
     * Inserisce l'attività di riepilogo delle operazioni effettuate nella tabella attivita
     *
     * @param azienda           l'azienda
     * @param persona           la persona
     * @param app               l'applicazione
     * @param buildedOperations le operazioni ribaltone
     * @param queryFactory      il query factory per le query
     */
    private void insertAttivita(Azienda azienda, Persona persona, Applicazione app, Operations buildedOperations, JPAQueryFactory queryFactory, String descrizioneErrore) {

        String oggetto = "E' stato aggiornato l'organigramma aziendale. Clicca per visualizzare il riepilogo delle operazioni effettuate.";
        if (descrizioneErrore != null) {
            oggetto = "Il tentivo di aggiornamento dell'organigranigramma aziendale è fallito. Clicca per visualizzare l'errore.";
        }
        Attivita a = new Attivita();
        a.setIdAzienda(azienda);
        a.setIdPersona(persona);
        a.setIdApplicazione(app);
        a.setTipo(Attivita.TipoAttivita.RIEPILOGO.toString().toLowerCase());
        a.setOggetto(oggetto);
        a.setDescrizione("Aggiornamento organigramma");
        a.setProvenienza("Ribaltone");
        entityManager.persist(a);
        entityManager.flush();
        insertDettagliAttivita(a.getId(), buildedOperations, queryFactory, descrizioneErrore);
    }

    /**
     * Inserisce i dettagli dell'attività suddivisi per sottosezione
     *
     * @param idAttivita        l'ID dell'attività
     * @param buildedOperations le operazioni ribaltone
     * @param queryFactory      il query factory per le query
     */
    private void insertDettagliAttivita(Integer idAttivita, Operations buildedOperations, JPAQueryFactory queryFactory, String descrizioneErrore) {
        List<DettaglioAttivita> list = new ArrayList<>();
        QPersona qPersona = QPersona.persona;
        QStruttura qStruttura = QStruttura.struttura;

        if (buildedOperations == null && descrizioneErrore != null) {
            DettaglioAttivita dettaglio = new DettaglioAttivita(
                idAttivita,
                DettaglioAttivita.SottosezioneDettaglioAttivita.ERRORE,
                descrizioneErrore,
                null,
                DettaglioAttivita.TipoOggettoDettaglioAttivita.MODIFICA_ORGANIGRAMMA,
                false
            );
            list.add(dettaglio);
        } else if (buildedOperations != null) {
            // ANAGRAFICHE
            if (buildedOperations.getListOfOperationAnagrafica() != null) {
                for (OperationAnagrafica operation : buildedOperations.getListOfOperationAnagrafica()) {
                    AnagraficaFields entita = resolveAnagraficaFields(operation.getEntitaCoinvolta());
                    if (entita == null) {
                        continue;
                    }
                    String descrizione = buildDescrizioneAnagrafica(operation, entita);
                    DettaglioAttivita dettaglio = new DettaglioAttivita(
                        idAttivita,
                        DettaglioAttivita.SottosezioneDettaglioAttivita.ANAGRAFICHE,
                        descrizione,
                        null,
                        DettaglioAttivita.TipoOggettoDettaglioAttivita.MODIFICA_ORGANIGRAMMA,
                        false
                    );
                    list.add(dettaglio);
                }
            }

            // APPARTENENTI
            if (buildedOperations.getListOfOperationAppartenente() != null) {
                for (OperationAppartenente operation : buildedOperations.getListOfOperationAppartenente()) {
                    AppartenenteFields entita = resolveAppartenenteFields(operation.getEntitaCoinvolta());
                    if (entita == null) {
                        continue;
                    }
                    String descrizione = buildDescrizioneAppartenente(operation, entita);
                    DettaglioAttivita dettaglio = new DettaglioAttivita(
                        idAttivita,
                        DettaglioAttivita.SottosezioneDettaglioAttivita.APPARTENENTI,
                        descrizione,
                        null,
                        DettaglioAttivita.TipoOggettoDettaglioAttivita.MODIFICA_ORGANIGRAMMA,
                        false
                    );
                    list.add(dettaglio);
                }
            }

            // STRUTTURE (escludendo CAMBIO_PADRE e RINOMINA che vanno nelle trasformazioni)
            if (buildedOperations.getListOfOperationStruttura() != null) {
                for (OperationStruttura operation : buildedOperations.getListOfOperationStruttura()) {
                    // Escludi CAMBIO_PADRE e RINOMINA che sono gestiti nelle trasformazioni
                    if (operation.getAzione() == Operation.Azione.CAMBIO_PADRE
                        || operation.getAzione() == Operation.Azione.RINOMINA) {
                        continue;
                    }

                    StrutturaFields entita = resolveStrutturaFields(operation.getEntitaCoinvolta());
                    if (entita == null) {
                        continue;
                    }

                    String descrizione = buildDescrizioneStruttura(operation, entita);
                    DettaglioAttivita dettaglio = new DettaglioAttivita(
                        idAttivita,
                        DettaglioAttivita.SottosezioneDettaglioAttivita.STRUTTURE,
                        descrizione,
                        null,
                        DettaglioAttivita.TipoOggettoDettaglioAttivita.MODIFICA_ORGANIGRAMMA,
                        false
                    );
                    list.add(dettaglio);

                }
            }

            // TRASFORMAZIONI (include CAMBIO_PADRE e RINOMINA dalle strutture)
            if (buildedOperations.getListOfOperationTrasformazione() != null) {
                for (OperationTrasformazione operation : buildedOperations.getListOfOperationTrasformazione()) {
                    TrasformazioneFields entita = resolveTrasformazioneFields(operation.getEntitaCoinvolta());
                    if (entita == null) {
                        continue;
                    }

                    String descrizione = buildDescrizioneTrasformazione(operation, entita);
                    DettaglioAttivita dettaglio = new DettaglioAttivita(
                        idAttivita,
                        DettaglioAttivita.SottosezioneDettaglioAttivita.TRASFORMAZIONI,
                        descrizione,
                        null,
                        DettaglioAttivita.TipoOggettoDettaglioAttivita.MODIFICA_ORGANIGRAMMA,
                        false
                    );
                    list.add(dettaglio);
                }
            }
        }
        // Salva tutti i dettagli usando entityManager
        for (DettaglioAttivita dettaglio : list) {
            entityManager.persist(dettaglio);
        }
        entityManager.flush();
    }

    /**
     * Costruisce la descrizione per un'operazione anagrafica
     */
    private String buildDescrizioneAnagrafica(OperationAnagrafica operation, AnagraficaFields entita) {
        Map<String, String> descrizioniAggiuntive = operation.getDescrizioniAggiuntive();
        String nomeCompleto = entita.getNomeCompleto();
        String cf = entita.getCodiceFiscale();
        String email = entita.getEmail();

        switch (operation.getAzione()) {
            case INSERT:
                return String.format("Nuovo indirizzo email %s per l'utente %s (%s)", email, nomeCompleto, cf);
            case EDIT:
                return String.format("Aggiornamento indirizzo email %s per l'utente %s (%s)", email, nomeCompleto, cf);
//            case DELETE:
//                return String.format("Rimozione indirizzo email %s per l'utente %s (%s)", email, nomeCompleto, cf);
            default:
                return String.format("Modifica anagrafica per l'utente %s (%s)", nomeCompleto, cf);
        }
    }

    /**
     * Costruisce la descrizione per un'operazione appartenente
     */
    private String buildDescrizioneAppartenente(OperationAppartenente operation, AppartenenteFields entita) {
        Map<String, String> descrizioniAggiuntive = operation.getDescrizioniAggiuntive();
        String nomeCompleto = entita.getNomeCompleto();
        String cf = entita.getCodiceFiscale();
        String tipoAppartenenza = "T".equals(entita.getTipoAppartenenza()) ? "direttamente" : "funzionalmente";
        String nomeCasella = descrizioniAggiuntive != null ? descrizioniAggiuntive.get("nomeCasella") : null;
        if (nomeCasella == null) {
            nomeCasella = descrizioniAggiuntive != null ? descrizioniAggiuntive.get("descrizioneCasella") : "struttura";
        }
        String idCasella = entita.getIdCasella() != null ? entita.getIdCasella().toString() : "";
        String responsabile = entita.isResponsabile() ? " come responsabile" : "";

        switch (operation.getAzione()) {
            case INSERT:
                return String.format("L'utente %s (%s) entra a far parte %s della struttura %s (%s)%s",
                    nomeCompleto, cf, tipoAppartenenza, nomeCasella, idCasella, responsabile);
            case EDIT:
                String descrizioneCasella = descrizioniAggiuntive != null ? descrizioniAggiuntive.get("descrizioneCasella") : nomeCasella;
                String tipoAppartenenzaFormattato = "T".equals(entita.getTipoAppartenenza()) ? "diretta" : "funzionale";
                String ruoloResponsabile = entita.isResponsabile()
                    ? "con ruolo di responsabile" : "senza ruolo di responsabile";
                return String.format("L'utente %s (%s) della struttura %s (%s) è stato aggiornato con afferenza %s %s",
                    nomeCompleto, cf, descrizioneCasella, idCasella, tipoAppartenenzaFormattato, ruoloResponsabile);
            case CHIUSURA:
                return String.format("L'utente %s (%s) termina la sua afferenza per la struttura %s (%s)",
                    nomeCompleto, cf, nomeCasella, idCasella);
            default:
                return String.format("Modifica appartenenza per l'utente %s (%s)", nomeCompleto, cf);
        }
    }

    /**
     * Costruisce la descrizione per un'operazione struttura
     */
    private String buildDescrizioneStruttura(OperationStruttura operation, StrutturaFields entita) {
        Map<String, String> descrizioniAggiuntive = operation.getDescrizioniAggiuntive();
        String descrizione = entita.getDescrizione();
        String idCasella = entita.getIdCasella() != null ? entita.getIdCasella().toString() : "";

        switch (operation.getAzione()) {
            case INSERT:
                String descrizioneCasellaPadre = descrizioniAggiuntive != null
                    ? descrizioniAggiuntive.get("descrizioneCasellaPadre") : "struttura padre";
                String idCasellaPadre = entita.getIdPadre() != null ? entita.getIdPadre().toString() : "";
                return String.format("Creazione struttura %s (%s) come figlia di %s (%s)",
                    descrizione, idCasella, descrizioneCasellaPadre, idCasellaPadre);
            case EDIT:
                return String.format("Aggiornamento della struttura %s (%s)", descrizione, idCasella);
            case CHIUSURA:
                return String.format("Spegnimento della struttura %s (%s)", descrizione, idCasella);
            default:
                return String.format("Modifica struttura %s (%s)", descrizione, idCasella);
        }
    }

    /**
     * Costruisce la descrizione per un'operazione trasformazione
     */
    private String buildDescrizioneTrasformazione(OperationTrasformazione operation, TrasformazioneFields entita) {
        Map<String, String> descrizioniAggiuntive = operation.getDescrizioniAggiuntive();

        switch (operation.getAzione()) {
            case CAMBIO_PADRE:
                String descrizioneCasella = descrizioniAggiuntive != null
                    ? descrizioniAggiuntive.get("descrizioneCasella") : "struttura";
                String descrizioneCasellaPadreVecchio = descrizioniAggiuntive != null
                    ? descrizioniAggiuntive.get("descrizioneCasellaPadreVecchio") : "padre vecchio";
                String idCasellaPadreVecchio = descrizioniAggiuntive != null
                    ? descrizioniAggiuntive.get("idCasellaPadreVecchio") : "";
                String descrizioneCasellaPadreNuovo = descrizioniAggiuntive != null
                    ? descrizioniAggiuntive.get("descrizioneCasellaPadreNuovo") : "padre nuovo";
                String idCasellaPadreNuovo = descrizioniAggiuntive != null
                    ? descrizioniAggiuntive.get("idCasellaPadreNuovo") : "";
                return String.format("Trasformazione per Trasferimento della struttura %s (%s) dal padre %s (%s) al padre %s (%s)",
                    descrizioneCasella, entita.getIdCasellaPartenza(), descrizioneCasellaPadreVecchio, idCasellaPadreVecchio,
                    descrizioneCasellaPadreNuovo, idCasellaPadreNuovo);
            case RINOMINA:
                String descrizioneCasellaVecchia = descrizioniAggiuntive != null
                    ? descrizioniAggiuntive.get("descrizioneCasellaVecchia") : "struttura";
                String descrizioneCasellaNuova = descrizioniAggiuntive != null
                    ? descrizioniAggiuntive.get("descrizioneCasellaNuova") : "struttura";
                return String.format("Trasformazione per Rinomina della struttura %s (%s) rinominata in %s",
                    descrizioneCasellaVecchia, entita.getIdCasellaPartenza(), descrizioneCasellaNuova);
            case CONFLUENZA:
                String descrizioneCasellaPartenza = descrizioniAggiuntive != null
                    ? descrizioniAggiuntive.get("descrizioneCasellaPartenza") : "struttura partenza";
                String descrizioneCasellaArrivo = descrizioniAggiuntive != null
                    ? descrizioniAggiuntive.get("descrizioneCasellaArrivo") : "struttura arrivo";
                return String.format("Trasformazione per Confluenza per la struttura %s (%s) nella struttura %s (%s)",
                    descrizioneCasellaPartenza, entita.getIdCasellaPartenza(),
                    descrizioneCasellaArrivo, entita.getIdCasellaArrivo());
            default:
                return String.format("Trasformazione struttura %s", entita.getIdCasellaPartenza());
        }
    }

    /**
     * Costruisce la descrizione per un'operazione trasformazione da struttura (CAMBIO_PADRE o RINOMINA)
     */
    private String buildDescrizioneTrasformazioneStruttura(OperationStruttura operation, StrutturaFields entita) {
        Map<String, String> descrizioniAggiuntive = operation.getDescrizioniAggiuntive();
        String descrizione = entita.getDescrizione();
        String idCasella = entita.getIdCasella() != null ? entita.getIdCasella().toString() : "";

        if (operation.getAzione() == Operation.Azione.CAMBIO_PADRE) {
            String descrizioneCasellaPadreVecchio = descrizioniAggiuntive != null
                ? descrizioniAggiuntive.get("descrizioneCasellaPadreVecchio") : "padre vecchio";
            String idCasellaPadreVecchio = descrizioniAggiuntive != null
                ? descrizioniAggiuntive.get("idCasellaPadreVecchio") : "";
            String descrizioneCasellaPadreNuovo = descrizioniAggiuntive != null
                ? descrizioniAggiuntive.get("descrizioneCasellaPadreNuovo") : "padre nuovo";
            String idCasellaPadreNuovo = descrizioniAggiuntive != null
                ? descrizioniAggiuntive.get("idCasellaPadreNuovo") : "";
            return String.format("Trasformazione per Trasferimento della struttura %s (%s) dal padre %s (%s) al padre %s (%s)",
                descrizione, idCasella, descrizioneCasellaPadreVecchio, idCasellaPadreVecchio,
                descrizioneCasellaPadreNuovo, idCasellaPadreNuovo);
        } else if (operation.getAzione() == Operation.Azione.RINOMINA) {
            String descrizioneCasellaVecchia = descrizioniAggiuntive != null
                ? descrizioniAggiuntive.get("descrizioneCasellaVecchia") : descrizione;
            return String.format("Trasformazione per Rinomina della struttura %s (%s) rinominata in %s",
                descrizioneCasellaVecchia, idCasella, descrizione);
        }
        return String.format("Trasformazione struttura %s (%s)", descrizione, idCasella);
    }

    private AnagraficaFields resolveAnagraficaFields(Object entita) {
        if (entita instanceof DatiDaImportareAnagrafica) {
            DatiDaImportareAnagrafica datiDaImportare = (DatiDaImportareAnagrafica) entita;
            return new AnagraficaFields(datiDaImportare.getNome(), datiDaImportare.getCognome(), datiDaImportare.getCodiceFiscale(), datiDaImportare.getEmail());
        }
        if (entita instanceof DatiImportatiAnagrafica) {
            DatiImportatiAnagrafica datiImportati = (DatiImportatiAnagrafica) entita;
            return new AnagraficaFields(datiImportati.getNome(), datiImportati.getCognome(), datiImportati.getCodiceFiscale(), datiImportati.getEmail());
        }
        log.warn("Entità anagrafica non gestita ({})", entita != null ? entita.getClass() : "null");
        return null;
    }

    private AppartenenteFields resolveAppartenenteFields(Object entita) {
        if (entita instanceof DatiDaImportareAppartenente) {
            DatiDaImportareAppartenente datiDaImportare = (DatiDaImportareAppartenente) entita;
            return new AppartenenteFields(
                datiDaImportare.getNome(),
                datiDaImportare.getCognome(),
                datiDaImportare.getCodiceFiscale(),
                datiDaImportare.getTipoAppartenenza(),
                datiDaImportare.getIdCasella(),
                datiDaImportare.getResponsabile()
            );
        }
        if (entita instanceof DatiImportatiAppartenente) {
            DatiImportatiAppartenente datiImportati = (DatiImportatiAppartenente) entita;
            return new AppartenenteFields(
                datiImportati.getNome(),
                datiImportati.getCognome(),
                datiImportati.getCodiceFiscale(),
                datiImportati.getTipoAppartenenza(),
                datiImportati.getIdCasella(),
                datiImportati.getResponsabile()
            );
        }
        log.warn("Entità appartenente non gestita ({})", entita != null ? entita.getClass() : "null");
        return null;
    }

    private StrutturaFields resolveStrutturaFields(Object entita) {
        if (entita instanceof DatiDaImportareStruttura) {
            DatiDaImportareStruttura datiDaImportare = (DatiDaImportareStruttura) entita;
            return new StrutturaFields(datiDaImportare.getDescrizione(), datiDaImportare.getIdCasella(), datiDaImportare.getIdPadre());
        }
        if (entita instanceof DatiImportatiStruttura) {
            DatiImportatiStruttura datiImportati = (DatiImportatiStruttura) entita;
            return new StrutturaFields(datiImportati.getDescrizione(), datiImportati.getIdCasella(), datiImportati.getIdPadre());
        }
        log.warn("Entità struttura non gestita ({})", entita != null ? entita.getClass() : "null");
        return null;
    }

    private TrasformazioneFields resolveTrasformazioneFields(Object entita) {
        if (entita instanceof DatiDaImportareTrasformazione) {
            DatiDaImportareTrasformazione datiDaImportare = (DatiDaImportareTrasformazione) entita;
            return new TrasformazioneFields(datiDaImportare.getIdCasellaPartenza(), datiDaImportare.getIdCasellaArrivo());
        }
        if (entita instanceof DatiImportatiTrasformazione) {
            DatiImportatiTrasformazione datiImportati = (DatiImportatiTrasformazione) entita;
            return new TrasformazioneFields(datiImportati.getIdCasellaPartenza(), datiImportati.getIdCasellaArrivo());
        }
        log.warn("Entità trasformazione non gestita ({})", entita != null ? entita.getClass() : "null");
        return null;
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private static final class AnagraficaFields {

        private final String nome;
        private final String cognome;
        private final String codiceFiscale;
        private final String email;

        private AnagraficaFields(String nome, String cognome, String codiceFiscale, String email) {
            this.nome = nome;
            this.cognome = cognome;
            this.codiceFiscale = codiceFiscale;
            this.email = email;
        }

        private String getNomeCompleto() {
            return (safe(cognome) + " " + safe(nome)).trim();
        }

        private String getCodiceFiscale() {
            return codiceFiscale;
        }

        private String getEmail() {
            return email;
        }
    }

    private static final class AppartenenteFields {

        private final String nome;
        private final String cognome;
        private final String codiceFiscale;
        private final String tipoAppartenenza;
        private final Integer idCasella;
        private final Boolean responsabile;

        private AppartenenteFields(String nome, String cognome, String codiceFiscale, String tipoAppartenenza, Integer idCasella, Boolean responsabile) {
            this.nome = nome;
            this.cognome = cognome;
            this.codiceFiscale = codiceFiscale;
            this.tipoAppartenenza = tipoAppartenenza;
            this.idCasella = idCasella;
            this.responsabile = responsabile;
        }

        private String getNomeCompleto() {
            return (safe(cognome) + " " + safe(nome)).trim();
        }

        private String getCodiceFiscale() {
            return codiceFiscale;
        }

        private String getTipoAppartenenza() {
            return tipoAppartenenza;
        }

        private Integer getIdCasella() {
            return idCasella;
        }

        private boolean isResponsabile() {
            return Boolean.TRUE.equals(responsabile);
        }
    }

    private static final class StrutturaFields {

        private final String descrizione;
        private final Integer idCasella;
        private final Integer idPadre;

        private StrutturaFields(String descrizione, Integer idCasella, Integer idPadre) {
            this.descrizione = descrizione;
            this.idCasella = idCasella;
            this.idPadre = idPadre;
        }

        private String getDescrizione() {
            return descrizione;
        }

        private Integer getIdCasella() {
            return idCasella;
        }

        private Integer getIdPadre() {
            return idPadre;
        }
    }

    private static final class TrasformazioneFields {

        private final Integer idCasellaPartenza;
        private final Integer idCasellaArrivo;

        private TrasformazioneFields(Integer idCasellaPartenza, Integer idCasellaArrivo) {
            this.idCasellaPartenza = idCasellaPartenza;
            this.idCasellaArrivo = idCasellaArrivo;
        }

        private Integer getIdCasellaPartenza() {
            return idCasellaPartenza;
        }

        private Integer getIdCasellaArrivo() {
            return idCasellaArrivo;
        }
    }

    /**
     * Invia la mail all'utente interessato per il riepilogo delle operazioni effettuate
     *
     * @param p                 la persona
     * @param buildedOperations le operazioni ribaltone (per generare il rapporto)
     */
    private void sendEmail(List<String> mails, Azienda a, Operations buildedOperations, String descrizioneErrore) {
        // TODO: Da fare in altra storia
        // Questo metodo sarà implementato in futuro per inviare una mail con lo stesso contenuto
        // delle attività create. Il codice è già strutturato per essere generico e riutilizzabile.
        String esito = "ESEGUITO RIBALTONE";
        if (descrizioneErrore != null) {
            esito = "ERRORE";
        }
        String subject = "Risultato aggiornamento organigramma: " + esito;

        String fromAlias = "Esito Aggiornamento Organigramma";
        String body = buildMailBody(buildedOperations, descrizioneErrore);
        AziendaParametriJson.MailParams mailParams = a.getParametri().getMailParams();
        simpleMailSenderUtility.sendMail(
            fromAlias,
            subject,
            mails,
            body,
            null,
            null,
            null,
            null,
            mailParams,
            true
        );

    }

    private String buildMailBody(Operations buildedOperations, String descrizioneErrore) {
        Map<String, String> mappa = new HashMap<String, String>();

        if (buildedOperations == null && descrizioneErrore != null) {

            mappa.put("Dettaglio Errore", descrizioneErrore);
        } else if (buildedOperations != null) {
            // ANAGRAFICHE
            if (buildedOperations.getListOfOperationAnagrafica() != null) {
                for (OperationAnagrafica operation : buildedOperations.getListOfOperationAnagrafica()) {
                    AnagraficaFields entita = resolveAnagraficaFields(operation.getEntitaCoinvolta());
                    if (entita == null) {
                        continue;
                    }
                    String descrizione = buildDescrizioneAnagrafica(operation, entita);

                    mappa.put("Modifiche Anagrafica", descrizione);
                }
            }

            // APPARTENENTI
            if (buildedOperations.getListOfOperationAppartenente() != null) {
                for (OperationAppartenente operation : buildedOperations.getListOfOperationAppartenente()) {
                    AppartenenteFields entita = resolveAppartenenteFields(operation.getEntitaCoinvolta());
                    if (entita == null) {
                        continue;
                    }
                    String descrizione = buildDescrizioneAppartenente(operation, entita);

                    mappa.put("Modifiche Appartenenti", descrizione);
                }
            }

            // STRUTTURE (escludendo CAMBIO_PADRE e RINOMINA che vanno nelle trasformazioni)
            if (buildedOperations.getListOfOperationStruttura() != null) {
                for (OperationStruttura operation : buildedOperations.getListOfOperationStruttura()) {
                    // Escludi CAMBIO_PADRE e RINOMINA che sono gestiti nelle trasformazioni
                    if (operation.getAzione() == Operation.Azione.CAMBIO_PADRE
                        || operation.getAzione() == Operation.Azione.RINOMINA) {
                        continue;
                    }

                    StrutturaFields entita = resolveStrutturaFields(operation.getEntitaCoinvolta());
                    if (entita == null) {
                        continue;
                    }

                    String descrizione = buildDescrizioneStruttura(operation, entita);

                    mappa.put("Modifiche Strutture", descrizione);

                }
            }

            // TRASFORMAZIONI (include CAMBIO_PADRE e RINOMINA dalle strutture)
            if (buildedOperations.getListOfOperationTrasformazione() != null) {
                for (OperationTrasformazione operation : buildedOperations.getListOfOperationTrasformazione()) {
                    TrasformazioneFields entita = resolveTrasformazioneFields(operation.getEntitaCoinvolta());
                    if (entita == null) {
                        continue;
                    }

                    String descrizione = buildDescrizioneTrasformazione(operation, entita);

                    mappa.put("Modifiche Trasformazioni", descrizione);
                }
            }
        }
        StringBuilder html = new StringBuilder();

        for (Map.Entry<String, String> entry : mappa.entrySet()) {
            html.append("<b>")
                .append(entry.getKey())
                .append("</b><br>")
                .append(entry.getValue())
                .append("<br><br>");
        }

        String result = html.toString();

        return result;
    }
}
