package it.bologna.ausl.internauta.utils.versatore.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * Classe astratta per la gestione del controllo di idoneità
 * I vari servizi che eseguono i versamenti dovranno implementare i metodi checkDocImpl e checkArchivioImpl per eseguire il 
 * controllo di idoneità
 * @author gdm
 */
public abstract class IdoneitaChecker {
    
    @PersistenceContext
    protected EntityManager entityManager;
    
    @Autowired
    protected VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;
        
    @Autowired
    protected TransactionTemplate transactionTemplate;
    
    @Autowired
    protected VersatoreConfigParams configParams;
    
    @Autowired
    protected ObjectMapper objectMapper;

    protected VersatoreConfiguration versatoreConfiguration;
    
    /**
     * Da implementare sul plugin. Deve eseguire il controllo di idoneità sul doc
     * @param id l'id del doc sul quale controllore l'idoneità
     * @param params i parametri di versamento letti da configurazione.parametri_aziende
     * @return true se il doc è idoneo, false altrimenti
     * @throws VersatoreProcessingException 
     */
    public abstract Boolean checkDocImpl(Integer id, Map<String,Object> params) throws VersatoreProcessingException;
    
    /**
     * Da implementare sul plugin. Deve eseguire il controllo di idoneità sull'archivio
     * @param id l'id dell'archivio sul quale controllore l'idoneità
     * @param params i parametri di versamento letti da configurazione.parametri_aziende
     * @return true se l'archivio è idoneo, false altrimenti
     * @throws VersatoreProcessingException 
     */
    public abstract Boolean checkArchivioImpl(Integer id, Map<String,Object> params) throws VersatoreProcessingException;
    
    /**
     * esegue il controllo di idoneità sul doc
     * @param id l'id del doc sul quale controllore l'idoneità
     * @param params i parametri di versamento letti da configurazione.parametri_aziende
     * @return true se il doc è idoneo, false altrimenti
     * @throws VersatoreProcessingException 
     */
    public Boolean checkDoc(Integer id, Map<String,Object> params) throws VersatoreProcessingException {
       transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(a -> {
            try {
                return checkDocImpl(id, params);
            } catch (VersatoreProcessingException ex) {
               throw new RuntimeException(ex);
            }
        });
    }
    
    /**
     * esegue il controllo di idoneità sull'archivio
     * @param id l'id dell'archivio sul quale controllore l'idoneità
     * @param params i parametri di versamento letti da configurazione.parametri_aziende
     * @return true se l'archivio è idoneo, false altrimenti
     * @throws VersatoreProcessingException 
     */
    public Boolean checkArchivio(Integer id, Map<String,Object> params) throws VersatoreProcessingException {
       transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(a -> {
            try {
                return checkArchivioImpl(id, params);
            } catch (VersatoreProcessingException ex) {
               throw new RuntimeException(ex);
            }
        });
    }
}
