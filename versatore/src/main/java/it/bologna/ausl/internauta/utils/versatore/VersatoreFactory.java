package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreConfigurationException;
import it.bologna.ausl.internauta.utils.versatore.repositories.ConfigurationRepository;
import it.bologna.ausl.internauta.utils.versatore.services.InfocertVersatoreService;
import it.bologna.ausl.internauta.utils.versatore.utils.VersatoreConfigParams;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Configuration
public class VersatoreFactory {
    
     // Elenco dei vari provider supportati
    public static enum VersatoreProviders {
        PARER, INFOCERT
    };
    
    @Autowired
    private VersatoreConfigParams versatoreConfigParams;
    
    // Contiene per ogni installazione (identificata dal suo hostId), un'istanza del Versatore
    private final static Map<String, VersatoreDocs> hostIdVersatoreInstansceMap = new HashMap<>();
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
    private ConfigurationRepository configurationRepository;
    
    
    List<it.bologna.ausl.model.entities.versatore.Configuration> configurations;
    
    private static final Logger logger = LoggerFactory.getLogger(VersatoreFactory.class);
    
    @PostConstruct
    public void initVersatoreFactory() throws VersatoreConfigurationException {
        configurations = configurationRepository.findAll();
        VersatoreDocs versatoreDocsInstance;
        for (it.bologna.ausl.model.entities.versatore.Configuration configuration : configurations) {
            VersatoreProviders provider = VersatoreProviders.valueOf(configuration.getProvider().getId());
            switch (provider) {
                case PARER:
                    // TODO
                    versatoreDocsInstance = null;
                    break;
                case INFOCERT:
                    versatoreDocsInstance = new InfocertVersatoreService(entityManager, versatoreConfigParams, configuration);
                    logger.info(configuration.getParams().toString());
                    break;
                default:
                    throw new VersatoreConfigurationException("Provider: " + provider + " not found");
            }
            hostIdVersatoreInstansceMap.put(configuration.getHostId(), versatoreDocsInstance);
        }
    }
    
    
    public VersatoreDocs getVersatoreDocsInstance(String hostId) {
        // Tramite l'hostId recupero dalla mappa l'istanza creata in fase di inizializzazione
        VersatoreDocs versatoreDocsInstance = hostIdVersatoreInstansceMap.get(hostId);
        return versatoreDocsInstance;
    }
}
