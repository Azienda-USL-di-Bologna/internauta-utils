package it.bologna.ausl.internauta.utils.ribaltone.utils;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAnagrafica;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationTrasformazione;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QStruttura;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.baborg.Struttura;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.QApplicazione;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareStruttura;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareTrasformazione;
import it.bologna.ausl.model.entities.scrivania.Attivita;
import it.bologna.ausl.model.entities.scrivania.DettaglioAttivita;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
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
    
    /*
     * Genera e invia le notifiche/mail agli utenti interessati per le operazioni ribaltone
     * @param buildedOperations le operazioni ribaltone
     * @param codiceAzienda il codice dell'azienda
     * @param idPersonaLanciante l'ID della persona che ha lanciato il ribaltone (null se automatico)
     * @param idPersoneDaNotificare lista di ID persone da notificare (per ribaltone automatico, null se manuale)
     */
    public void generaAndInviaNotifiche(Operations buildedOperations, String codiceAzienda, List<Integer> idPersoneDaNotificare) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QAzienda qAzienda = QAzienda.azienda;
        Azienda azienda = queryFactory.select(qAzienda).from(qAzienda).where(qAzienda.codice.eq(codiceAzienda)).fetchOne();
        
        QApplicazione qApplicazione = QApplicazione.applicazione;
        Applicazione app = queryFactory.select(qApplicazione).from(qApplicazione).where(qApplicazione.nome.eq(Applicazione.Applicazioni.ribaltorg.toString())).fetchOne();
        
        // Ricavare le persone da notificare: 
        // in caso di ribaltone manuale è la persona che ha lanciato il ribaltone manuale, 
        // in caso di ribaltone automatico sono le persone scritte nella configurazione di ribaltone
        List<Persona> personeDaNotificareSuScrivania = new ArrayList<>();
        List<Persona> personeDaNotificareTeamiteMail = new ArrayList<>();
        
        QPersona qPersona = QPersona.persona;
        
        if (idPersoneDaNotificare != null && !idPersoneDaNotificare.isEmpty()) {
            // Ribaltone automatico: notificare le persone dalla configurazione
            List<Persona> persone = queryFactory.select(qPersona).from(qPersona).where(qPersona.id.in(idPersoneDaNotificare)).fetch();
            personeDaNotificareSuScrivania.addAll(persone);
        }
        
        // Genera le attività per ogni persona
        for (Persona p : personeDaNotificareSuScrivania) {
            insertAttivita(azienda, p, app, buildedOperations, queryFactory);
        }
        for (Persona p : personeDaNotificareTeamiteMail) {
            sendEmail(p, buildedOperations);
        }
    }
    
    /**
     * Inserisce l'attività di riepilogo delle operazioni effettuate nella tabella attivita
     * @param azienda l'azienda
     * @param persona la persona
     * @param app l'applicazione
     * @param buildedOperations le operazioni ribaltone
     * @param queryFactory il query factory per le query
     */
    private void insertAttivita(Azienda azienda, Persona persona, Applicazione app, Operations buildedOperations, JPAQueryFactory queryFactory) {
        String oggetto = "E' stato aggiornato l'organigramma aziendale. Clicca per visualizzare il riepilogo delle operazioni effettuate.";
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
        insertDettagliAttivita(a.getId(), buildedOperations, queryFactory);
    }
    
    /**
     * Inserisce i dettagli dell'attività suddivisi per sottosezione
     * @param idAttivita l'ID dell'attività
     * @param buildedOperations le operazioni ribaltone
     * @param queryFactory il query factory per le query
     */
    private void insertDettagliAttivita(Integer idAttivita, Operations buildedOperations, JPAQueryFactory queryFactory) {
        List<DettaglioAttivita> list = new ArrayList<>();
        QPersona qPersona = QPersona.persona;
        QStruttura qStruttura = QStruttura.struttura;
        
        // ANAGRAFICHE
        if (buildedOperations.getListOfOperationAnagrafica() != null) {
            for (OperationAnagrafica operation : buildedOperations.getListOfOperationAnagrafica()) {
                DatiDaImportareAnagrafica entita = (DatiDaImportareAnagrafica) operation.getEntitaCoinvolta();
                // Ricarica la persona dal DB per ottenere l'ID (le operazioni sono detached dalla cache)
                Persona persona = queryFactory.select(qPersona)
                    .from(qPersona)
                    .where(qPersona.codiceFiscale.eq(entita.getCodiceFiscale())
                        .and(qPersona.attiva.isTrue()))
                    .fetchOne();
                
                if (persona != null) {
                    String descrizione = buildDescrizioneAnagrafica(operation, entita);
                    DettaglioAttivita dettaglio = new DettaglioAttivita(
                        idAttivita,
                        DettaglioAttivita.SottosezioneDettaglioAttivita.ANAGRAFICHE,
                        descrizione,
                        persona.getId(),
                        DettaglioAttivita.TipoOggettoDettaglioAttivita.MODIFICA_ORGANIGRAMMA,
                        false
                    );
                    list.add(dettaglio);
                }
            }
        }
        
        // APPARTENENTI
        if (buildedOperations.getListOfOperationAppartenente() != null) {
            for (OperationAppartenente operation : buildedOperations.getListOfOperationAppartenente()) {
                DatiDaImportareAppartenente entita = (DatiDaImportareAppartenente) operation.getEntitaCoinvolta();
                // Ricarica la persona dal DB per ottenere l'ID
                Persona persona = queryFactory.select(qPersona)
                    .from(qPersona)
                    .where(qPersona.codiceFiscale.eq(entita.getCodiceFiscale())
                        .and(qPersona.attiva.isTrue()))
                    .fetchOne();
                
                if (persona != null) {
                    String descrizione = buildDescrizioneAppartenente(operation, entita);
                    DettaglioAttivita dettaglio = new DettaglioAttivita(
                        idAttivita,
                        DettaglioAttivita.SottosezioneDettaglioAttivita.APPARTENENTI,
                        descrizione,
                        persona.getId(),
                        DettaglioAttivita.TipoOggettoDettaglioAttivita.MODIFICA_ORGANIGRAMMA,
                        false
                    );
                    list.add(dettaglio);
                }
            }
        }
        
        // STRUTTURE (escludendo CAMBIO_PADRE e RINOMINA che vanno nelle trasformazioni)
        if (buildedOperations.getListOfOperationStruttura() != null) {
            for (OperationStruttura operation : buildedOperations.getListOfOperationStruttura()) {
                // Escludi CAMBIO_PADRE e RINOMINA che sono gestiti nelle trasformazioni
                if (operation.getAzione() == Operation.Azione.CAMBIO_PADRE || 
                    operation.getAzione() == Operation.Azione.RINOMINA) {
                    continue;
                }
                
                DatiDaImportareStruttura entita = (DatiDaImportareStruttura) operation.getEntitaCoinvolta();
                // Ricarica la struttura dal DB per ottenere l'ID
                Struttura struttura = queryFactory.select(qStruttura)
                    .from(qStruttura)
                    .where(qStruttura.idCasella.eq(entita.getIdCasella())
                        .and(qStruttura.attiva.isTrue())
                        .and(qStruttura.idAzienda.id.eq(entita.getIdAzienda())))
                    .fetchOne();
                
                if (struttura != null) {
                    String descrizione = buildDescrizioneStruttura(operation, entita);
                    DettaglioAttivita dettaglio = new DettaglioAttivita(
                        idAttivita,
                        DettaglioAttivita.SottosezioneDettaglioAttivita.STRUTTURE,
                        descrizione,
                        struttura.getId(),
                        DettaglioAttivita.TipoOggettoDettaglioAttivita.MODIFICA_ORGANIGRAMMA,
                        false
                    );
                    list.add(dettaglio);
                }
            }
        }
        
        // TRASFORMAZIONI (include CAMBIO_PADRE e RINOMINA dalle strutture)
        if (buildedOperations.getListOfOperationTrasformazione() != null) {
            for (OperationTrasformazione operation : buildedOperations.getListOfOperationTrasformazione()) {
                DatiDaImportareTrasformazione entita = (DatiDaImportareTrasformazione) operation.getEntitaCoinvolta();
                // Per le trasformazioni, usa la struttura di partenza
                Struttura struttura = queryFactory.select(qStruttura)
                    .from(qStruttura)
                    .where(qStruttura.idCasella.eq(entita.getIdCasellaPartenza())
                        .and(qStruttura.attiva.isTrue())
                        .and(qStruttura.idAzienda.id.eq(entita.getIdAzienda())))
                    .fetchOne();
                
                // Se non trovata attiva, cerca quella disattivata più recente
                if (struttura == null) {
                    struttura = queryFactory.select(qStruttura)
                        .from(qStruttura)
                        .where(qStruttura.idCasella.eq(entita.getIdCasellaPartenza())
                            .and(qStruttura.idAzienda.id.eq(entita.getIdAzienda())))
                        .orderBy(qStruttura.dataCessazione.desc())
                        .limit(1)
                        .fetchOne();
                }
                
                if (struttura != null) {
                    String descrizione = buildDescrizioneTrasformazione(operation, entita);
                    DettaglioAttivita dettaglio = new DettaglioAttivita(
                        idAttivita,
                        DettaglioAttivita.SottosezioneDettaglioAttivita.TRASFORMAZIONI,
                        descrizione,
                        struttura.getId(),
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
    private String buildDescrizioneAnagrafica(OperationAnagrafica operation, DatiDaImportareAnagrafica entita) {
        Map<String, String> descrizioniAggiuntive = operation.getDescrizioniAggiuntive();
        String nomeCompleto = entita.getCognome() + " " + entita.getNome();
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
    private String buildDescrizioneAppartenente(OperationAppartenente operation, DatiDaImportareAppartenente entita) {
        Map<String, String> descrizioniAggiuntive = operation.getDescrizioniAggiuntive();
        String nomeCompleto = entita.getCognome() + " " + entita.getNome();
        String cf = entita.getCodiceFiscale();
        String tipoAppartenenza = "T".equals(entita.getTipoAppartenenza()) ? "direttamente" : "funzionalmente";
        String nomeCasella = descrizioniAggiuntive != null ? descrizioniAggiuntive.get("nomeCasella") : null;
        if (nomeCasella == null) {
            nomeCasella = descrizioniAggiuntive != null ? descrizioniAggiuntive.get("descrizioneCasella") : "struttura";
        }
        String idCasella = entita.getIdCasella() != null ? entita.getIdCasella().toString() : "";
        String responsabile = entita.getResponsabile() != null && entita.getResponsabile() ? " come responsabile" : "";
        
        switch (operation.getAzione()) {
            case INSERT:
                return String.format("L'utente %s (%s) entra a far parte %s della struttura %s (%s)%s", 
                    nomeCompleto, cf, tipoAppartenenza, nomeCasella, idCasella, responsabile);
            case EDIT:
                String descrizioneCasella = descrizioniAggiuntive != null ? descrizioniAggiuntive.get("descrizioneCasella") : nomeCasella;
                String tipoAppartenenzaFormattato = "T".equals(entita.getTipoAppartenenza()) ? "diretta" : "funzionale";
                String ruoloResponsabile = entita.getResponsabile() != null && entita.getResponsabile() ? 
                    "con ruolo di responsabile" : "senza ruolo di responsabile";
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
    private String buildDescrizioneStruttura(OperationStruttura operation, DatiDaImportareStruttura entita) {
        Map<String, String> descrizioniAggiuntive = operation.getDescrizioniAggiuntive();
        String descrizione = entita.getDescrizione();
        String idCasella = entita.getIdCasella() != null ? entita.getIdCasella().toString() : "";
        
        switch (operation.getAzione()) {
            case INSERT:
                String descrizioneCasellaPadre = descrizioniAggiuntive != null ? 
                    descrizioniAggiuntive.get("descrizioneCasellaPadre") : "struttura padre";
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
    private String buildDescrizioneTrasformazione(OperationTrasformazione operation, DatiDaImportareTrasformazione entita) {
        Map<String, String> descrizioniAggiuntive = operation.getDescrizioniAggiuntive();
        
        switch (operation.getAzione()) {
            case CAMBIO_PADRE:
                String descrizioneCasella = descrizioniAggiuntive != null ? 
                    descrizioniAggiuntive.get("descrizioneCasella") : "struttura";
                String descrizioneCasellaPadreVecchio = descrizioniAggiuntive != null ? 
                    descrizioniAggiuntive.get("descrizioneCasellaPadreVecchio") : "padre vecchio";
                String idCasellaPadreVecchio = descrizioniAggiuntive != null ? 
                    descrizioniAggiuntive.get("idCasellaPadreVecchio") : "";
                String descrizioneCasellaPadreNuovo = descrizioniAggiuntive != null ? 
                    descrizioniAggiuntive.get("descrizioneCasellaPadreNuovo") : "padre nuovo";
                String idCasellaPadreNuovo = descrizioniAggiuntive != null ? 
                    descrizioniAggiuntive.get("idCasellaPadreNuovo") : "";
                return String.format("Trasformazione per Trasferimento della struttura %s (%s) dal padre %s (%s) al padre %s (%s)", 
                    descrizioneCasella, entita.getIdCasellaPartenza(), descrizioneCasellaPadreVecchio, idCasellaPadreVecchio, 
                    descrizioneCasellaPadreNuovo, idCasellaPadreNuovo);
            case RINOMINA:
                String descrizioneCasellaVecchia = descrizioniAggiuntive != null ? 
                    descrizioniAggiuntive.get("descrizioneCasellaVecchia") : "struttura";
                String descrizioneCasellaNuova = descrizioniAggiuntive != null ? 
                    descrizioniAggiuntive.get("descrizioneCasellaNuova") : "struttura";
                return String.format("Trasformazione per Rinomina della struttura %s (%s) rinominata in %s", 
                    descrizioneCasellaVecchia, entita.getIdCasellaPartenza(), descrizioneCasellaNuova);
            case CONFLUENZA:
                String descrizioneCasellaPartenza = descrizioniAggiuntive != null ? 
                    descrizioniAggiuntive.get("descrizioneCasellaPartenza") : "struttura partenza";
                String descrizioneCasellaArrivo = descrizioniAggiuntive != null ? 
                    descrizioniAggiuntive.get("descrizioneCasellaArrivo") : "struttura arrivo";
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
    private String buildDescrizioneTrasformazioneStruttura(OperationStruttura operation, DatiDaImportareStruttura entita) {
        Map<String, String> descrizioniAggiuntive = operation.getDescrizioniAggiuntive();
        String descrizione = entita.getDescrizione();
        String idCasella = entita.getIdCasella() != null ? entita.getIdCasella().toString() : "";
        
        if (operation.getAzione() == Operation.Azione.CAMBIO_PADRE) {
            String descrizioneCasellaPadreVecchio = descrizioniAggiuntive != null ? 
                descrizioniAggiuntive.get("descrizioneCasellaPadreVecchio") : "padre vecchio";
            String idCasellaPadreVecchio = descrizioniAggiuntive != null ? 
                descrizioniAggiuntive.get("idCasellaPadreVecchio") : "";
            String descrizioneCasellaPadreNuovo = descrizioniAggiuntive != null ? 
                descrizioniAggiuntive.get("descrizioneCasellaPadreNuovo") : "padre nuovo";
            String idCasellaPadreNuovo = descrizioniAggiuntive != null ? 
                descrizioniAggiuntive.get("idCasellaPadreNuovo") : "";
            return String.format("Trasformazione per Trasferimento della struttura %s (%s) dal padre %s (%s) al padre %s (%s)", 
                descrizione, idCasella, descrizioneCasellaPadreVecchio, idCasellaPadreVecchio, 
                descrizioneCasellaPadreNuovo, idCasellaPadreNuovo);
        } else if (operation.getAzione() == Operation.Azione.RINOMINA) {
            String descrizioneCasellaVecchia = descrizioniAggiuntive != null ? 
                descrizioniAggiuntive.get("descrizioneCasellaVecchia") : descrizione;
            return String.format("Trasformazione per Rinomina della struttura %s (%s) rinominata in %s", 
                descrizioneCasellaVecchia, idCasella, descrizione);
        }
        return String.format("Trasformazione struttura %s (%s)", descrizione, idCasella);
    }
    
    /**
     * Invia la mail all'utente interessato per il riepilogo delle operazioni effettuate
     * @param p la persona
     * @param buildedOperations le operazioni ribaltone (per generare il rapporto)
     */
    private void sendEmail(Persona p, Operations buildedOperations) {
        // TODO: Da fare in altra storia
        // Questo metodo sarà implementato in futuro per inviare una mail con lo stesso contenuto
        // delle attività create. Il codice è già strutturato per essere generico e riutilizzabile.
    }
}
