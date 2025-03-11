package it.bologna.ausl.internauta.utils.ribaltone.operation;

import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import static it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation.Azione.INSERT;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.QPersona;
import it.bologna.ausl.model.entities.baborg.QUtente;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAnagrafica;
import it.bologna.ausl.model.entities.ribaltonedati.DatiDaImportareAppartenente;
import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author Top
 */
public class OperationAnagrafica extends Operation<DatiRibaltoneInterface> implements Serializable {
    
    private final QPersona qPersona = QPersona.persona;
    private final QUtente qUtente = QUtente.utente;
    
    public OperationAnagrafica(Azione azione, DatiRibaltoneInterface entitaCoinvolta, EntityManager entityManager) {
        super(azione, entitaCoinvolta, entityManager);
    }
    
    @Override
    public void esegui(Object workToDo, RepositoryFactory repositoryFactory) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(getEntityManager());
        switch (getAzione()) {
            
            case EDIT:
            case INSERT: {
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
                if (utente != null && entitaDaInserireOModificare.getEmail() != null) {
                    String nuovaEmail = entitaDaInserireOModificare.getEmail();
                    if (utente.getEmails() != null) {
                        // Se esiste già un array di email, ne creiamo uno nuovo con una dimensione maggiore
                        String[] nuoveEmails = Arrays.copyOf(utente.getEmails(), utente.getEmails().length + 1);
                        nuoveEmails[nuoveEmails.length - 1] = nuovaEmail; // Aggiungiamo la nuova email
                        utente.setEmails(nuoveEmails);
                    } else {
                        // Se non esiste, creiamo un nuovo array con la sola nuova email
                        utente.setEmails(new String[]{nuovaEmail});
                    }
                    getEntityManager().persist(utente);
                }
                
                break;
            }
        }
    }
    
}
