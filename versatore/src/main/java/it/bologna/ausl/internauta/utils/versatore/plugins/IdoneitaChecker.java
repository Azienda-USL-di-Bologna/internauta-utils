package it.bologna.ausl.internauta.utils.versatore.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
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
    
    public abstract Boolean checkDocImpl(Integer id) throws VersatoreProcessingException;
    
    public abstract Boolean checkArchivioImpl(Integer id) throws VersatoreProcessingException;
    
    public Boolean checkDoc(Integer id) throws VersatoreProcessingException {
       transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(a -> {
            try {
                return checkDocImpl(id);
            } catch (VersatoreProcessingException ex) {
               throw new RuntimeException(ex);
            }
        });
    }
    
    public Boolean checkArchivio(Integer id) throws VersatoreProcessingException {
       transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(a -> {
            try {
                return checkArchivioImpl(id);
            } catch (VersatoreProcessingException ex) {
               throw new RuntimeException(ex);
            }
        });
    }
}
