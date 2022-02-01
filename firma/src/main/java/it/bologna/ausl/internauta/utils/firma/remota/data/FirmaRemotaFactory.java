package it.bologna.ausl.internauta.utils.firma.remota.data;

import it.bologna.ausl.internauta.utils.firma.remota.configuration.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.remota.data.FirmaRemotaInformation.FirmaRemotaProviders;
import static it.bologna.ausl.internauta.utils.firma.remota.data.FirmaRemotaInformation.FirmaRemotaProviders.ARUBA;
import it.bologna.ausl.internauta.utils.firma.remota.data.arubasignservice.FirmaRemotaAruba;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.http.FirmaRemotaException;
import it.bologna.ausl.internauta.utils.firma.remota.utils.FirmaRemotaUtils;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
/**
 *
 * @author gdm
 */
@Configuration
public class FirmaRemotaFactory {

    private final static Map<FirmaRemotaProviders, FirmaRemota> providersMap = new HashMap<>();
    private static final Object LOCK = new Object();
    
    @Autowired
    private ConfigParams configParams;
//
//    @Autowired
//    @Qualifier("TLSClient")
//    private HttpClient httpClient;

    @Autowired
    private FirmaRemotaUtils firmaRemotaUtils;

    @Value("${arubasignservice.dominio_firma:FRauslbo}")
    private String dominioFirmaDefault;

//    @PostConstruct
//    public void initFirmaRemotaFactory() throws FirmaRemotaException {
//        providersMap = new HashMap<>();
//        for (FirmaRemotaProviders p: FirmaRemotaProviders.values()) {
//            FirmaRemota firmaRemotaInstance = instanceFirmaRemotaByProvider(p);
//            providersMap.put(p, firmaRemotaInstance);
//        }
//    }

    /** torna l'istanza della classe giusta in base al provider passato
     * 
     * @param provider il provider di firma remota (es. ARUBA)
     * @return l'istanza della classe giusta in base al provider passato
     * @throws FirmaRemotaException 
     */
    public FirmaRemota getFirmaRemotaInstance(FirmaRemotaProviders provider) throws FirmaRemotaException {
        FirmaRemota firmaRemotaInstance = null;
        switch (provider) {
            case ARUBA: 
                // se nella mappa delle instanze delle classi dei provider non c'è l'istanza...
                if (providersMap.get(FirmaRemotaProviders.ARUBA) == null) {
                    
                    //... devo inserire l'istanza. Devo sincronizzarmi su un oggetto per ottenere un lock ed evitare problemi di concorrenza nell'accesso a una mappa statica
                    synchronized (LOCK) {

                        //NB: si poteva evitare l'if precedente, ma così facendo, se l'istanza è presente si prenderebbe un lock inutile
                        if (providersMap.get(FirmaRemotaProviders.ARUBA) == null) {
                            firmaRemotaInstance = new FirmaRemotaAruba(configParams, firmaRemotaUtils, dominioFirmaDefault);
                            providersMap.put(FirmaRemotaProviders.ARUBA, firmaRemotaInstance);
                        }
                    }
                }
                
                // torno l'istanza prendendola dalla mappa
                firmaRemotaInstance = providersMap.get(FirmaRemotaProviders.ARUBA);
                break;
            default:
                throw new FirmaRemotaException("Provider: " + provider + " not found");
        }
        return firmaRemotaInstance;
    }
}