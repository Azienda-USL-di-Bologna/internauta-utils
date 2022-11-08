package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreConfigurationException;
import it.bologna.ausl.internauta.utils.versatore.repositories.ConfigurationRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
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
    
     // elenco dei vari provider supportati
    public static enum VersatoreProviders {
        PARER, INFOCERT
    };
    
    // contiene per ogni istallazione (identificata dal suo hostId), una istanza di FirmaRemota
    private final static Map<String, Object> hostIdVersatoreInstansceMap = new HashMap<>();
    
    @Autowired
    private ConfigurationRepository configurationRepository;
    
    List<it.bologna.ausl.model.entities.versatore.Configuration> configurations;
    
    private static final Logger logger = LoggerFactory.getLogger(VersatoreFactory.class);
    
    @PostConstruct
    public void initVersatoreFactory() throws VersatoreConfigurationException {
        configurations = configurationRepository.findAll();
        
        for (it.bologna.ausl.model.entities.versatore.Configuration configuration : configurations) {
            VersatoreProviders provider = VersatoreProviders.valueOf(configuration.getProvider().getId());
            switch (provider) {
                case PARER:
                    // TODO
                    break;
                case INFOCERT:
                    // TODO
                    logger.info(configuration.getParams().toString());
                    break;
                default:
                    throw new VersatoreConfigurationException("Provider: " + provider + " not found");
            }
            hostIdVersatoreInstansceMap.put(configuration.getHostId(), "TODO");
        }
    }
    
}
