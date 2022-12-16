package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public abstract class VersatoreDocs {
    
    @PersistenceContext
    protected EntityManager entityManager;
    
    @Autowired
    protected VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;
        
    @Autowired
    protected TransactionTemplate transactionTemplate;
    
    @Autowired
    protected VersatoreConfigParams configParams;

    protected VersatoreConfiguration versatoreConfiguration;
    
    protected MinIOWrapper minIOWrapper;

    public void init(VersatoreConfiguration versatoreConfiguration) {
        minIOWrapper = versatoreRepositoryConfiguration.getVersatoreRepositoryManager().getMinIOWrapper();
        this.versatoreConfiguration = versatoreConfiguration;
    }

    
    public VersamentoDocInformation versa(VersamentoDocInformation versamentoInformation) throws VersatoreProcessingException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(a -> {
            try {
                return versaImpl(versamentoInformation);
            } catch (VersatoreProcessingException ex) {
               throw new RuntimeException(ex);
            }
        });
    }
    
    protected abstract VersamentoDocInformation versaImpl(VersamentoDocInformation versamentoInformation) throws VersatoreProcessingException;
    
}
