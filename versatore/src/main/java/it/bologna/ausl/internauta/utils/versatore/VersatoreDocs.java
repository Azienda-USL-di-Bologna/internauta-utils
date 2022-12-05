package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreConfigurationException;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import javax.persistence.EntityManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public abstract class VersatoreDocs {
    
    protected final EntityManager entityManager;
    protected final TransactionTemplate transactionTemplate;
    protected final VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;
    protected final VersatoreConfigParams configParams;
    protected final VersatoreConfiguration configuration;

    protected VersatoreDocs(EntityManager entityManager, TransactionTemplate transactionTemplate, VersatoreRepositoryConfiguration versatoreRepositoryConfiguration, 
            VersatoreConfigParams configParams, VersatoreConfiguration configuration) {
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
        this.versatoreRepositoryConfiguration = versatoreRepositoryConfiguration;
        this.configParams = configParams;
        this.configuration = configuration;
    }
    

    public VersamentoDocInformation versa(VersamentoDocInformation versamentoInformation) throws VersatoreConfigurationException {
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionTemplate.execute(a -> {
            try {
                return versaAbstract(versamentoInformation);
            } catch (VersatoreConfigurationException ex) {
               throw new RuntimeException(ex);
            }
        });
    }
    
    protected abstract VersamentoDocInformation versaAbstract(VersamentoDocInformation versamentoInformation) throws VersatoreConfigurationException;
}
