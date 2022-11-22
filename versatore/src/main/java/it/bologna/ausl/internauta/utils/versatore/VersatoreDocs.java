package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.internauta.utils.versatore.configuration.VersatoreRepositoryConfiguration;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreConfigurationException;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import javax.persistence.EntityManager;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public abstract class VersatoreDocs {
    
    protected final EntityManager entityManager;
    protected final VersatoreRepositoryConfiguration versatoreRepositoryConfiguration;
    protected final VersatoreConfigParams configParams;
    protected final VersatoreConfiguration configuration;

    protected VersatoreDocs(EntityManager entityManager, VersatoreRepositoryConfiguration versatoreRepositoryConfiguration, 
            VersatoreConfigParams configParams, VersatoreConfiguration configuration) {
        this.entityManager = entityManager;
        this.versatoreRepositoryConfiguration = versatoreRepositoryConfiguration;
        this.configParams = configParams;
        this.configuration = configuration;
    }
    
    
    public abstract VersamentoInformation versa(VersamentoInformation versamentoInformation) throws VersatoreConfigurationException;
}
