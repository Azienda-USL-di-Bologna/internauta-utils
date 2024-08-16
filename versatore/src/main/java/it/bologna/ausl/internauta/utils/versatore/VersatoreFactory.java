package it.bologna.ausl.internauta.utils.versatore;

import it.bologna.ausl.internauta.utils.versatore.plugins.VersatoreDocs;
import it.bologna.ausl.internauta.utils.versatore.exceptions.VersatoreProcessingException;
import it.bologna.ausl.internauta.utils.versatore.plugins.IdoneitaChecker;
import it.bologna.ausl.internauta.utils.versatore.plugins.infocert.InfocertIdoneitaCheckerService;
import it.bologna.ausl.internauta.utils.versatore.plugins.infocert.InfocertVersatoreService;
import it.bologna.ausl.internauta.utils.versatore.plugins.parer.ParerIdoneitaCheckerService;
import it.bologna.ausl.internauta.utils.versatore.plugins.parer.ParerVersatoreService;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.SdicoIdoneitaCheckerService;
import it.bologna.ausl.internauta.utils.versatore.plugins.sdico.SdicoVersatoreService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import it.bologna.ausl.internauta.utils.versatore.repositories.VersatoreConfigurationRepository;
import it.bologna.ausl.model.entities.versatore.VersatoreConfiguration;
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
        PARER, INFOCERT, SDICO
    };

    @Autowired
    private BeanFactory beanFactory;
    
    @Autowired
    @Qualifier("VersatoreConfigurationRepository")
    private VersatoreConfigurationRepository configurationRepository;
    
    private boolean initialized = false;
    
    // Contiene per ogni installazione (identificata dal suo hostId), un'istanza del Versatore
    private final static Map<String, VersatoreDocs> hostIdVersatoreInstansceMap = new HashMap<>();
    
    // Contiene per ogni installazione (identificata dal suo hostId), un'istanza del checker dell'idoneità
    private final static Map<String, IdoneitaChecker> hostIdIdoneitaCheckerInstanceMap = new HashMap<>();
   
    List<VersatoreConfiguration> configurations;
    
    private static final Logger logger = LoggerFactory.getLogger(VersatoreFactory.class);
    
    /**
     * inizializza le classi necessarie al versatore
     * Sia per il controllo idoneità che per il versamento, crea una mappa che ha come chiave l'hostId e come valore
     * l'istanza della classe del plugin corretta
     * @throws VersatoreProcessingException 
     */
    public void initVersatoreFactory() throws VersatoreProcessingException {
        configurations = configurationRepository.findAll();
        VersatoreDocs versatoreDocsInstance;
        IdoneitaChecker idoneitaCheckerInstance;
        for (VersatoreConfiguration configuration : configurations) {
            VersatoreProviders provider = VersatoreProviders.valueOf(configuration.getProvider().getId());
            switch (provider) {
                case PARER:
                    versatoreDocsInstance = beanFactory.getBean(ParerVersatoreService.class);
                    versatoreDocsInstance.init(configuration);
                    idoneitaCheckerInstance = beanFactory.getBean(ParerIdoneitaCheckerService.class);
                    break;
                case INFOCERT:
                    versatoreDocsInstance = beanFactory.getBean(InfocertVersatoreService.class);
                    versatoreDocsInstance.init(configuration);
                    idoneitaCheckerInstance = beanFactory.getBean(InfocertIdoneitaCheckerService.class);;
                    break;
                case SDICO:
                    versatoreDocsInstance = beanFactory.getBean(SdicoVersatoreService.class);
                    versatoreDocsInstance.init(configuration);
                    idoneitaCheckerInstance = beanFactory.getBean(SdicoIdoneitaCheckerService.class);;
                    break;
                default:
                    throw new VersatoreProcessingException("Provider: " + provider + " not found");
            }
            hostIdVersatoreInstansceMap.put(configuration.getHostId(), versatoreDocsInstance);
            hostIdIdoneitaCheckerInstanceMap.put(configuration.getHostId(), idoneitaCheckerInstance);
        }
        initialized = true;
    }
    
    /**
     * Torna l'istanza della classe per il versamento relativa all'hostId passato
     * @param hostId
     * @return l'istanza della classe per il versamento relativa all'hostId passato
     * @throws VersatoreProcessingException 
     */
    public VersatoreDocs getVersatoreDocsInstance(String hostId) throws VersatoreProcessingException {
        // Tramite l'hostId recupero dalla mappa l'istanza creata in fase di inizializzazione
        if (!initialized) {
            initVersatoreFactory();
        }
        VersatoreDocs versatoreDocsInstance = hostIdVersatoreInstansceMap.get(hostId);
        return versatoreDocsInstance;
    }
    
    /**
     * Torna l'istanza della classe per il controllo idoneità all'hostId passato
     * @param hostId
     * @return l'istanza della classe per il controllo idoneità all'hostId passato
     * @throws VersatoreProcessingException 
     */
    public IdoneitaChecker getIdoneitaCheckerInstance(String hostId) throws VersatoreProcessingException {
        // Tramite l'hostId recupero dalla mappa l'istanza creata in fase di inizializzazione
        if (!initialized) {
            initVersatoreFactory();
        }
        IdoneitaChecker idoneitaChecker = hostIdIdoneitaCheckerInstanceMap.get(hostId);
        return idoneitaChecker;
    }
}
