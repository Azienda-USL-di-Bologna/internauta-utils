package it.bologna.ausl.internauta.utils.ribaltone.operation;

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
        switch (getAzione()) {

            case EDIT, INSERT -> {
                DatiDaImportareAnagrafica entitaDaInserireOModificare = (DatiDaImportareAnagrafica) getEntitaCoinvolta();
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
                DatiImportatiAnagrafica anagraficaImportata = queryFactory
                    .select(qDatiImportatiAnagrafica)
                    .from(qDatiImportatiAnagrafica)
                    .where(qDatiImportatiAnagrafica.codiceFiscale.eq(entitaDaInserireOModificare.getCodiceFiscale()).and(qDatiImportatiAnagrafica.idAzienda.eq(entitaDaInserireOModificare.getIdAzienda()))).fetchOne();
                if (utente != null && entitaDaInserireOModificare.getEmail() != null) {
                    String oldMail = "";
                    if (anagraficaImportata != null) {
                        oldMail = anagraficaImportata.getEmail();
                    }
                    String nuovaEmail = entitaDaInserireOModificare.getEmail();
                    if (utente.getEmails() != null) {
                        String[] nuoveEmails = Arrays.copyOf(utente.getEmails(), utente.getEmails().length);

                        if (nuovaEmail != null && !nuovaEmail.equals("")) {
                            int indexOldMail = Arrays.binarySearch(nuoveEmails, oldMail);
                            if (indexOldMail >= 0) {
                                nuoveEmails[indexOldMail] = nuovaEmail;
                            } else {
                                nuoveEmails = Arrays.copyOf(utente.getEmails(), utente.getEmails().length + 1);
                                nuoveEmails[nuoveEmails.length - 1] = nuovaEmail;
                            }
                        } else {
                            ArrayList<String> lista = new ArrayList<>(Arrays.asList(utente.getEmails()));
                            lista.remove(oldMail);
                            nuoveEmails = lista.toArray(String[]::new);
                        }
                        utente.setEmails(nuoveEmails);

                    } else {
                        // Se non esiste, creiamo un nuovo array con la sola nuova email
                        utente.setEmails(new String[]{nuovaEmail});
                    }
                    repositoryFactory.getEntityManager().persist(utente);
                }
            }
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
                DatiImportatiAnagrafica datiImportatiAnagrafica = queryFactory.select(qDatiImportatiAnagrafica).from(qDatiImportatiAnagrafica).where(qDatiImportatiAnagrafica.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale()).and(qDatiImportatiAnagrafica.idAzienda.eq(entitaDaInserire.getIdAzienda()))).fetchOne();
                if (StringUtils.hasText(entitaDaInserire.getEmail())) {
                    Persona p = queryFactory.select(qPersona).from(qPersona).where(qPersona.attiva.and(qPersona.codiceFiscale.eq(entitaDaInserire.getCodiceFiscale()))).fetchOne();
                    if (p != null && datiImportatiAnagrafica != null) {
                        Contatto c = p.getIdContatto();
                        List<DettaglioContatto> dcList = c.getDettaglioContattoList().stream().filter(dc -> dc.getDescrizione().equals(datiImportatiAnagrafica.getEmail())).toList();
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
                        } else if (c.getDettaglioContattoList() != null && !c.getDettaglioContattoList().isEmpty()) {
                            List<Utente> utentiList = p.getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(entitaDaInserire.getIdAzienda())).toList();
                            if (utentiList.size() == 1) {
                                Utente u = utentiList.get(0);
                                List<DettaglioContatto> dc = u.buildDettagliContattoEmail(c);
                                c.getDettaglioContattoList().addAll(dc);
                                entityManager.persist(c);
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
                        if (c != null && c.getDettaglioContattoList() != null) {
                            //List<DettaglioContatto> dcList = c.getDettaglioContattoList().stream().filter(dc -> dc.getDescrizione().equals(entitaDaInserire.getEmail())).toList();
                            List<Utente> utentiList = p.getUtenteList().stream().filter(u -> u.getIdAzienda().getId().equals(entitaDaInserire.getIdAzienda())).toList();
                            if (utentiList.size() == 1) {
                                Utente u = utentiList.get(0);
                                List<DettaglioContatto> dc = u.buildDettagliContattoEmail(c);
                                c.getDettaglioContattoList().addAll(dc);
                                entityManager.persist(c);
                            }

                        }
                    }
                }
                break;
            }
        }
    }

}
