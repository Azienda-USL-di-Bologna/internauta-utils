package it.bologna.ausl.internauta.utils.firma.remota;

import it.bologna.ausl.internauta.utils.firma.remota.configuration.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation.FirmaRemotaProviders;
import static it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation.FirmaRemotaProviders.ARUBA;
import it.bologna.ausl.internauta.utils.firma.remota.arubasignservice.FirmaRemotaAruba;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.infocertsignservice.FirmaRemotaInfocert;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import it.bologna.ausl.internauta.utils.firma.repositories.ConfigurationRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author gdm
 */
@Configuration
public class FirmaRemotaFactory {

    // contiene per ogni istallazione (identificata dal suo hostId), una istanza di FirmaRemota
    private final static Map<String, FirmaRemota> hostIdFirmaInstansceMap = new HashMap<>();

    @Autowired
    private ConfigParams configParams;

    @Autowired
    private FirmaRemotaDownloaderUtils firmaRemotaUtils;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private InternalCredentialManager internalCredentialManager;

    List<it.bologna.ausl.model.entities.firma.Configuration> configurations;

    @Value("${arubasignservice.dominio_firma:FRauslbo}")
    private String dominioFirmaDefault;

    @Value("${firma.remota.infocert.ssl.certpath:#{null}}")
    private String firmaRemotaInfocertSslCertpath;

    @Value("${firma.remota.infocert.ssl.certpassword:#{null}}")
    private String firmaRemotaInfocertSslCertpassword;

    @PostConstruct
    public void initFirmaRemotaFactory() throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        configurations = configurationRepository.findAll();
        for (it.bologna.ausl.model.entities.firma.Configuration configuration : configurations) {
            FirmaRemotaProviders provider = FirmaRemotaProviders.valueOf(configuration.getProvider().getId());
            FirmaRemota firmaRemotaInstance;
            switch (provider) {
                case ARUBA:
                    firmaRemotaInstance = new FirmaRemotaAruba(configParams, firmaRemotaUtils, configuration, internalCredentialManager, dominioFirmaDefault);
                    break;
                case INFOCERT:
                    firmaRemotaInstance = new FirmaRemotaInfocert(configParams, firmaRemotaUtils, configuration, internalCredentialManager, firmaRemotaInfocertSslCertpath, firmaRemotaInfocertSslCertpassword);
                    break;
                default:
                    throw new FirmaRemotaConfigurationException("Provider: " + provider + " not found");
            }
            hostIdFirmaInstansceMap.put(configuration.getHostId(), firmaRemotaInstance);
        }
    }

    /**
     * Torna l'istanza corretta dell'implementazione di FirmaRemota relativa
     * all'hostId passato.
     *
     * @param hostId l'hostId della tabella Configurations che identifica
     * l'installazione della firma remota da utilizzare
     * @return l'istanza della classe giusta in base all'hostId passato
     * @throws FirmaRemotaHttpException
     * @throws
     * it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException
     */
    public FirmaRemota getFirmaRemotaInstance(String hostId) throws FirmaRemotaHttpException, FirmaRemotaConfigurationException {
        // tramite l'hostId recupero dalla mappa l'istanza creta in fase di inizializzazione
        FirmaRemota firmaRemotaInstance = hostIdFirmaInstansceMap.get(hostId);
        return firmaRemotaInstance;
    }
}
