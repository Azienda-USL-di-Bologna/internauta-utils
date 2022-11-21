package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
import javax.persistence.EntityManager;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public abstract class VersatoreDocs {
    
    protected final EntityManager entityManager;
    protected final VersatoreConfigParams configParams;
    protected final VersatoreConfiguration configuration;

    protected VersatoreDocs(EntityManager entityManager, VersatoreConfigParams configParams, VersatoreConfiguration configuration) {
        this.entityManager = entityManager;
        this.configParams = configParams;
        this.configuration = configuration;
    }
    
    
    public abstract VersamentoInformation versa(VersamentoInformation versamentoInformation);
}
