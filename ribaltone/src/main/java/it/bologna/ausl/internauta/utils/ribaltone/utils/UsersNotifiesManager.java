package it.bologna.ausl.internauta.utils.ribaltone.utils;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.model.entities.baborg.Azienda;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QAzienda;
import it.bologna.ausl.model.entities.configurazione.Applicazione;
import it.bologna.ausl.model.entities.configurazione.QApplicazione;
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
     */
    public void generaAndInviaNotifiche(Operations buildedOperations, String codiceAzienda) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QAzienda qAzienda = QAzienda.azienda;
        Azienda azienda = queryFactory.select(qAzienda).from(qAzienda).where(qAzienda.codice.eq(codiceAzienda)).fetchOne();
        
        QApplicazione qApplicazione = QApplicazione.applicazione;
        Applicazione app = queryFactory.select(qApplicazione).from(qApplicazione).where(qApplicazione.nome.eq(Applicazione.Applicazioni.ribaltorg.toString())).fetchOne();
        
        // TODO: Come ricavo le persone da notificare? + la persoan che ha lanciato il ribaltone + quelle configurarate per doverla ricevere ?
        List<Persona> personeDaNotificareSuScrivania = new ArrayList();
        List<Persona> personeDaNotificareTeamiteMail = new ArrayList();
        
        for (Persona p : personeDaNotificareSuScrivania) {
            insertAttivita(azienda, p, app);
        }
        for (Persona p : personeDaNotificareTeamiteMail) {
            sendEmail(p);
        }
    }
    
    /**
     * Inserisce l'attività di riepilogo delle operazioni effettuate nella tabella attivita
     * @param azienda l'azienda
     * @param persona la persona
     * @param app l'applicazione
     * @param tipoAttivita il tipo di attività
     * @return l'id dell'attività
     */
    private void insertAttivita(Azienda azienda, Persona persona, Applicazione app) {
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
        insertDettagliAttivita(a.getId(), null, null);
    }
    
    private void insertDettagliAttivita(Integer idAttivita, List<Integer> idArchivioEffettivamenteCoinvolto, Map<Integer, Object> infoArchivio) {
        List<DettaglioAttivita> list = new ArrayList<>();

        for (Integer id : idArchivioEffettivamenteCoinvolto) {
            DettaglioAttivita.SottosezioneDettaglioAttivita sottosezione = DettaglioAttivita.SottosezioneDettaglioAttivita.ANAGRAFICHE;
            
            DettaglioAttivita a = new DettaglioAttivita(
                    idAttivita, 
                    sottosezione, 
                    "TODO: azione",
                    id, 
                    DettaglioAttivita.TipoOggettoDettaglioAttivita.MODIFICA_ORGANIGRAMMA, 
                    false
            );
            list.add(a);
        }

        dettaglioAttivitaRepository.saveAll(list);
    }
    
    /**
     * Invia la mail all'utente interessato per il riepilogo delle operazioni effettuate
     * @param p la persona
     */
    private void sendEmail(Persona p) {
        // TODO: Da fare in altra storia
    }
}
