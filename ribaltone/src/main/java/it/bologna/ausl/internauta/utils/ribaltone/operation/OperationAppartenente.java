package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.blackbox.PermissionManager;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.baborg.Persona;
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
import jakarta.persistence.PersistenceContext;
import java.io.Serializable;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Top
 */
public class OperationAppartenente extends Operation<DatiRibaltoneInterface> implements Serializable {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PermissionManager permissionManager;

    private final QPersona qPersona = QPersona.persona;
    private final QUtente qUtente = QUtente.utente;
    private final QUtenteStruttura qUtenteStruttura = QUtenteStruttura.utenteStruttura;
    private final QStruttura qStruttura = QStruttura.struttura;

    public OperationAppartenente(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager) {
        super(azione, entitaCoinvolta, entityManager);

    }

    @Override
    public void esegui(Object workToDo) throws RibaltoneHttpException {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);

        switch (getAzione()) {

            case INSERT: {
                DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                //faccio fetchFirst perche mi aspetto di trovare una sola persona con quel codice fiscale o di non trovarne affatto
                UtenteStruttura utenteStruttura = new UtenteStruttura();
                Struttura struttura = getStruttura(queryFactory, entitaDaInserire);
                Persona persona = getPersona(queryFactory, entitaDaInserire);
                Utente utente;
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
                    utenteStruttura.setResponsabile(entitaDaInserire.getResposabile());

                } else {
                    throw new RibaltoneHttpException(" non trovata la struttura di un utente qualcosa nei controlli è andato male!!!");
                }
                //inserire in baborg persone se non c'è la persona
                //inserire in baborg utenti se non c'è l'utente
                //inserire in baborg utenti struttura se non c'è l'afferenza ricordandosi di una sola afferenza diretta e n funzionali
                entityManager.persist(utenteStruttura);
                //inserire i permessi di flusso per i responsabili
            }
            break;

            case CHIUSURA: {
                DatiImportatiAppartenente entitaDaChiudere = (DatiImportatiAppartenente) getEntitaCoinvolta();
                Persona persona = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaChiudere.getCodiceFiscale()).and(qPersona.attiva)).fetchFirst();
                if (persona != null) {
                    getUtente(queryFactory, entitaDaChiudere, persona);
                }
                //chiudere in baborg utenti struttura
                //chiudere in baborg utenti se non ci sono afferenze attive
                //chiudere in baborg persone se non ci sono utenti attivi
                //chiudere i permessi di flusso e veicolati per la struttura di riferimento
            }
            break;
            case EDIT: {
                //sicuramente non ha cambiato struttura perche questa operazione si traduce in una insert e una chiusura
                DatiDaImportareAppartenente entitaDaInserire = (DatiDaImportareAppartenente) getEntitaCoinvolta();
                Persona persona = queryFactory.select(qPersona).from(qPersona).where(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale())).fetchFirst();
                // è il caso di utente che diventa o non è più responsabile,
                // quindi verificare i permessi di flusso

                //che cambia nome
                // verificare baborg.persona
                //che cambia tipologia di afferenza
                //verificare baborg.utenti_strutttura
                //che cambia username
                //modificare username su baborg.utenti
                break;
            }
        }
    }

    private Struttura getStruttura(JPAQueryFactory queryFactory, DatiRibaltoneInterface entitaDaInserire) {
        return queryFactory
                .select(qStruttura)
                .from(qStruttura)
                .where(qStruttura.idCasella.eq(entitaDaInserire.getIdCasella())
                        .and(qStruttura.attiva)
                        .and(qStruttura.idAzienda.id.eq(entitaDaInserire.getIdAzienda())))
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

    private Persona getPersona(JPAQueryFactory queryFactory, DatiDaImportareAppartenente entitaDaInserire) {
        return queryFactory
                .select(qPersona)
                .from(qPersona)
                .where(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale()))
                .fetchFirst();
    }

}
