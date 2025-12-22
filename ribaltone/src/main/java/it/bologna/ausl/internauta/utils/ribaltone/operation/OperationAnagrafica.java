package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.EDIT;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.INSERT;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.Persona;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiImportatiAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.QDatiImportatiAnagrafica;
import it.bologna.ausl.model.entities.rubrica.Contatto;
import it.bologna.ausl.model.entities.rubrica.DettaglioContatto;
import it.bologna.ausl.model.entities.rubrica.Email;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 *
 * @author Top
 */
public class OperationAnagrafica extends Operation<DatiRibaltoneInterface> implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(OperationAnagrafica.class);
    private final QPersona qPersona = QPersona.persona;
    private final QUtente qUtente = QUtente.utente;
    private final QDatiImportatiAnagrafica qDatiImportatiAnagrafica = QDatiImportatiAnagrafica.datiImportatiAnagrafica;

    public OperationAnagrafica(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager, Map<String, String> descrizioniAggiuntive) {
        super(azione, entitaCoinvolta, entityManager, descrizioniAggiuntive);
    }

    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(repositoryFactory.getEntityManager());
        DatiDaImportareAnagrafica entitaDaInserireOModificare = (DatiDaImportareAnagrafica) getEntitaCoinvolta();
        log.info("inizio a gestire la mail di cf "
            + entitaDaInserireOModificare.getCodiceFiscale()
            + " codice matricola "
            + entitaDaInserireOModificare.getCodiceMatricola()
            + " codice ente "
            + entitaDaInserireOModificare.getCodiceEnte()
        );

        Utente utente = queryFactory
            .select(qUtente)
            .from(qPersona)
            .join(qUtente).on(qUtente.idPersona.eq(qPersona))
            .where(
                qPersona.codiceFiscale.eq(entitaDaInserireOModificare.getCodiceFiscale())
                    .and(
                        qUtente.idAzienda.id.eq(entitaDaInserireOModificare.getIdAzienda()).and(qUtente.attivo)
                    )
            )
            .fetchOne();
        if (utente != null) {

            switch (getAzione()) {

                case EDIT -> {
                    if (entitaDaInserireOModificare.getEmail() != null) {
                        String nuovaEmail = entitaDaInserireOModificare.getEmail();
                        DatiImportatiAnagrafica anagraficaVecchia = queryFactory.select(qDatiImportatiAnagrafica)
                            .from(qDatiImportatiAnagrafica)
                            .where(
                                qDatiImportatiAnagrafica.codiceFiscale.eq(entitaDaInserireOModificare.getCodiceFiscale()).and(
                                    qDatiImportatiAnagrafica.codiceMatricola.eq(entitaDaInserireOModificare.getCodiceMatricola())).and(
                                    qDatiImportatiAnagrafica.codiceEnte.eq(entitaDaInserireOModificare.getCodiceEnte()))
                            ).fetchOne();
                        if (anagraficaVecchia != null && anagraficaVecchia.getEmail() != null) {
                            String oldEmail = anagraficaVecchia.getEmail();
                            if (utente.getEmails() == null) {
                                String[] arrayList = new String[1];
                                utente.setEmails(arrayList);
                            }
                            utente.setEmails(
                                Stream.concat(Arrays.stream(utente.getEmails()).filter(e -> e != null && !e.equals(oldEmail)), // rimuove il vecchio elemento
                                    Stream.of(nuovaEmail) // aggiunge in coda il nuovo
                                ).toArray(String[]::new));
                        }

                    }
                }

                case INSERT -> {
                    if (entitaDaInserireOModificare.getEmail() != null && !entitaDaInserireOModificare.getEmail().equals("")) {
                        String nuovaEmail = entitaDaInserireOModificare.getEmail();
                        String[] nuoveEmails = null;
                        if (utente.getEmails() != null) {
                            nuoveEmails = Stream.concat(Arrays.stream(utente.getEmails()), Stream.of(nuovaEmail))
                                .filter(Objects::nonNull)
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .distinct()
                                .toArray(String[]::new);

                        } else {
                            nuoveEmails = List.of(nuovaEmail).toArray(new String[0]);
                        }
                        utente.setEmails(nuoveEmails);
                    }
                }
            }
            repositoryFactory.getEntityManager().persist(utente);
        }
    }

    public void menageContatto(RepositoryFactory repositoryFactory) throws RibaltoneHttpException {
        //devo creare i dettagli contatti corretti
        EntityManager entityManager = repositoryFactory.getEntityManager();
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QDatiImportatiAnagrafica qDatiImportatiAnagrafica = QDatiImportatiAnagrafica.datiImportatiAnagrafica;

        switch (getAzione()) {

            case EDIT -> {
                DatiDaImportareAnagrafica entitaDaInserire = (DatiDaImportareAnagrafica) getEntitaCoinvolta();
                DatiImportatiAnagrafica datiImportatiAnagrafica
                    = queryFactory.select(qDatiImportatiAnagrafica)
                        .from(qDatiImportatiAnagrafica)
                        .where(qDatiImportatiAnagrafica.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale())
                            .and(qDatiImportatiAnagrafica.idAzienda.eq(entitaDaInserire.getIdAzienda()))
                            .and(qDatiImportatiAnagrafica.codiceMatricola.eq(entitaDaInserire.getCodiceMatricola())))
                        .fetchOne();
                if (StringUtils.hasText(entitaDaInserire.getEmail())) {
                    Persona p = queryFactory.select(qPersona).from(qPersona).where(qPersona.attiva.and(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale()))).fetchOne();
                    if (p != null && datiImportatiAnagrafica != null) {
                        Contatto c = p.getIdContatto();
                        if (c == null) {
                            Persona ribaltone = queryFactory.select(qPersona).from(qPersona).where(qPersona.attiva.and(qPersona.codiceFiscale.eq("RIBALTONE"))).fetchOne();
                            c = p.buildContatto(
                                new Integer[]{entitaDaInserire.getIdAzienda()},
                                ribaltone,
                                ribaltone.getUtenteList().stream().filter(user -> user.getIdAzienda().getId().equals(entitaDaInserire.getIdAzienda())).toList().get(0)
                            );
                        }
                        List<DettaglioContatto> dcList = null;
                        if (c.getDettaglioContattoList() != null) {
                            dcList = c.getDettaglioContattoList().stream().filter(dc -> dc.getDescrizione().equals(datiImportatiAnagrafica.getEmail())).toList();
                        } else {
                            dcList = new ArrayList<>();
                        }
                        if (dcList != null && !dcList.isEmpty()) {
                            DettaglioContatto dc = dcList.get(0);
                            Email email = dc.getEmail();
                            email.setDescrizione(entitaDaInserire.getEmail());
                            email.setEmail(entitaDaInserire.getEmail());
                            email.setPec(false);
                            email.setIdContatto(c);
                            email.setIdDettaglioContatto(dc);
                            dc.setEmail(email);
                            dc.setDescrizione(entitaDaInserire.getEmail());
                            entityManager.persist(dc);
                            entityManager.flush();
                        } else if (c.getDettaglioContattoList() != null && !c.getDettaglioContattoList().isEmpty()) {
                            List<Utente> utentiList = p.getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(entitaDaInserire.getIdAzienda())).toList();
                            if (utentiList.size() == 1) {
                                Utente u = utentiList.get(0);
                                List<DettaglioContatto> dc = u.buildDettagliContattoEmail(c);
                                if (dc != null) {
                                    for (DettaglioContatto dettaglioContatto : dc) {
                                        List<DettaglioContatto> doppione = c.getDettaglioContattoList().stream().filter(dec -> dec.getDescrizione().equalsIgnoreCase(dettaglioContatto.getDescrizione())).toList();
                                        if (doppione != null && doppione.isEmpty()) {
                                            c.getDettaglioContattoList().add(dettaglioContatto);
                                        }
                                    }
                                    log.info("sto salvando il contatto con descrizione" + c.getDescrizione() + "con dettaglio contatto " + dc.get(0).getDescrizione());
                                    entityManager.persist(c);
                                    entityManager.flush();
                                }
                            }
                        }
                    }
                }
                break;
            }

            case INSERT -> {
                DatiDaImportareAnagrafica entitaDaInserire = (DatiDaImportareAnagrafica) getEntitaCoinvolta();

                if (StringUtils.hasText(entitaDaInserire.getEmail())) {
                    Persona p = queryFactory.select(qPersona).from(qPersona).where(qPersona.attiva.and(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale()))).fetchOne();
                    if (p != null) {
                        Contatto c = p.getIdContatto();
                        log.info("gestisco la persona " + p.getDescrizione() + " con cf: " + p.getCodiceFiscale());

                        if (c != null && c.getDettaglioContattoList() != null && p.getUtenteList() != null) {
                            //List<DettaglioContatto> dcList = c.getDettaglioContattoList().stream().filter(dc -> dc.getDescrizione().equals(entitaDaInserire.getEmail())).toList();
                            List<Utente> utentiList = p.getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(entitaDaInserire.getIdAzienda())).toList();
                            if (utentiList.size() == 1) {
                                Utente u = utentiList.get(0);
                                List<DettaglioContatto> dc = u.buildDettagliContattoEmail(c);
                                if (dc != null) {

                                    c.getDettaglioContattoList().addAll(dc);
                                    entityManager.persist(c);
                                }
                            }

                        }
                    }
                }
                break;
            }
        }
    }

}
