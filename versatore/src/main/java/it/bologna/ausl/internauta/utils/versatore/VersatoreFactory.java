package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.infocert.InfocertVersatoreService;
import it.bologna.ausl.internauta.utils.versatore.plugins.parer.ParerVersatoreService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import it.bologna.ausl.internauta.utils.versatore.repositories.VersatoreConfigurationRepository;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
@Component
public class VersatoreFactory {
    
     // Elenco dei vari provider supportati
    public static enum VersatoreProviders {
        PARER, INFOCERT
    };

    @Autowired
    private BeanFactory beanFactory;
    
    @Autowired
    @Qualifier("VersatoreConfigurationRepository")
    private VersatoreConfigurationRepository configurationRepository;
    
    private boolean initialized = false;
    
    // Contiene per ogni installazione (identificata dal suo hostId), un'istanza del Versatore
    private final static Map<String, VersatoreDocs> hostIdVersatoreInstansceMap = new HashMap<>();
   
    List<it.bologna.ausl.model.entities.versatore.VersatoreConfiguration> configurations;
    
    private static final Logger logger = LoggerFactory.getLogger(VersatoreFactory.class);
    
    public void initVersatoreFactory() throws VersatoreProcessingException {
        configurations = configurationRepository.findAll();
        VersatoreDocs versatoreDocsInstance;
        for (it.bologna.ausl.model.entities.versatore.VersatoreConfiguration configuration : configurations) {
            VersatoreProviders provider = VersatoreProviders.valueOf(configuration.getProvider().getId());
            switch (provider) {
                case PARER:
                    versatoreDocsInstance = beanFactory.getBean(ParerVersatoreService.class);
                    versatoreDocsInstance.init(configuration);
//                    versatoreDocsInstance = null;
                    break;
                case INFOCERT:
                    versatoreDocsInstance = beanFactory.getBean(InfocertVersatoreService.class);
                    versatoreDocsInstance.init(configuration);
                    break;
                default:
                    throw new VersatoreProcessingException("Provider: " + provider + " not found");
            }
            hostIdVersatoreInstansceMap.put(configuration.getHostId(), versatoreDocsInstance);
        }
        initialized = true;
    }
    
    
    public VersatoreDocs getVersatoreDocsInstance(String hostId) throws VersatoreProcessingException {
        // Tramite l'hostId recupero dalla mappa l'istanza creata in fase di inizializzazione
        if (!initialized) {
            initVersatoreFactory();
        }
        VersatoreDocs versatoreDocsInstance = hostIdVersatoreInstansceMap.get(hostId);
        return versatoreDocsInstance;
    }
}
