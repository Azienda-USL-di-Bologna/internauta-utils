package it.bologna.ausl.internauta.utils.firma.remota;

import it.bologna.ausl.internauta.utils.firma.remota.configuration.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation.FirmaRemotaProviders;
import static it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation.FirmaRemotaProviders.ARUBA;
import it.bologna.ausl.internauta.utils.firma.remota.arubasignservice.FirmaRemotaAruba;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaException;
import it.bologna.ausl.internauta.utils.firma.remota.infocertsignservice.FirmaRemotaInfocert;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaDownloaderUtils;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
/**
 *
 * @author gdm
 */
@Configuration
public class FirmaRemotaFactory {

    // mappa che contiene la mappa dei provider per ogni azienda
    private final static Map<String, Map<FirmaRemotaProviders, FirmaRemota>> aziendaProvidersMap = new HashMap<>();
    private static final Object LOCK = new Object();
    
    @Autowired
    private ConfigParams configParams;
    
    @Autowired
    private FirmaRemotaDownloaderUtils firmaRemotaUtils;
    
    /**
    * Questa mappa è un Bean di parameters-manager e viene popolata dall'applicazione contenitore (es. internauta) implementando it.bologna.ausl.internauta.utils.parameters.manager.configuration.ParametersManagerConfiguration
    * Contiene la mappa codice-azienda->id-azienda
    */
    @Resource
    @Qualifier("codiceAziendaIdAziendaMap")
    private Map<String, Object> codiceAziendaIdAziendaMap;

    @Value("${arubasignservice.dominio_firma:FRauslbo}")
    private String dominioFirmaDefault;

    @PostConstruct
    public void initFirmaRemotaFactory() throws FirmaRemotaException {
//        providersMap = new HashMap<>();
//        for (FirmaRemotaProviders p: FirmaRemotaProviders.values()) {
//            FirmaRemota firmaRemotaInstance = instanceFirmaRemotaByProvider(p);
//            providersMap.put(p, firmaRemotaInstance);
//        }
        codiceAziendaIdAziendaMap.keySet().stream().forEach(codAzienda -> {
            aziendaProvidersMap.put(codAzienda, new HashMap<>());
        });
    }

    /** 
     * Torna l'istanza della classe giusta in base al provider passato.
     * Ogni azienda ha la sua mappa dei provider, perché ogni azienda ha bisogno di un'istanza per ogni provider. 
     * Ad esempio ogni azienda potrebbe avere un host del servizio diverso a cui collegarsi
     * 
     * @param provider il provider di firma remota (es. ARUBA)
     * @param codiceAzienda il codice dell'azienda per la quale si vuole agire
     * @return l'istanza della classe giusta in base al provider passato
     * @throws FirmaRemotaException 
     * @throws it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException 
     */
    public FirmaRemota getFirmaRemotaInstance(FirmaRemotaProviders provider, String codiceAzienda) throws FirmaRemotaException, FirmaRemotaConfigurationException {
        FirmaRemota firmaRemotaInstance = null;
        
        // prima recupero la mappa dell'azienda interessata
        Map<FirmaRemotaProviders, FirmaRemota> providerMap = aziendaProvidersMap.get(codiceAzienda);
        
        // poi recupero l'istanza dell'implementazione corretta di FirmaRemota in base al provider
        switch (provider) {
            case ARUBA: 
                // se nella mappa delle instanze delle classi dei provider non c'è l'istanza...
                if (providerMap.get(FirmaRemotaProviders.ARUBA) == null) {
                    
                    //... devo inserire l'istanza. Devo sincronizzarmi su un oggetto per ottenere un lock ed evitare problemi di concorrenza nell'accesso a una mappa statica
                    synchronized (LOCK) {

                        //NB: si poteva evitare l'if precedente, ma così facendo, se l'istanza è presente si prenderebbe un lock inutile
                        if (providerMap.get(FirmaRemotaProviders.ARUBA) == null) {
                            firmaRemotaInstance = new FirmaRemotaAruba(codiceAzienda, configParams, firmaRemotaUtils, dominioFirmaDefault);
                            providerMap.put(FirmaRemotaProviders.ARUBA, firmaRemotaInstance);
                        }
                    }
                }
                
                // torno l'istanza prendendola dalla mappa
                firmaRemotaInstance = providerMap.get(FirmaRemotaProviders.ARUBA);
                break;
            case INFOCERT:
                 if (providerMap.get(FirmaRemotaProviders.INFOCERT) == null) {
                    synchronized (LOCK) {
                        if (providerMap.get(FirmaRemotaProviders.INFOCERT) == null) {
                            firmaRemotaInstance = new FirmaRemotaInfocert(codiceAzienda, configParams, firmaRemotaUtils);
                            providerMap.put(FirmaRemotaProviders.INFOCERT, firmaRemotaInstance);     
                        }
                    }
                 }
                break;
            default:
                throw new FirmaRemotaException("Provider: " + provider + " not found");
        }
        return firmaRemotaInstance;
    }
}