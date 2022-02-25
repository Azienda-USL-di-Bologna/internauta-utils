package it.bologna.ausl.internauta.utils.firma.remota.configuration;



import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.stereotype.Service;

/**
 * Questa classe legge i parametri di configurazione dal database (tabella configurazione.parametri_azienda)
 * La lettura viene effettuata tramite il modulo parametriAziendeReader.
 * 
 * @author gdm
 */
@Service
public class ConfigParams {

    private static Logger logger = LoggerFactory.getLogger(ConfigParams.class);

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ParametriAziendeReader parametriAziendeReader;

//    Map<String, Map<String, Object>> firmaRemotaConfiguration;
    private List<ParametroAziende> firmaRemotaConfigurationParams;
    
    private MinIOWrapper minIOWrapper;
    
    /*
    * Questa mappa è un Bean di parameters-manager e viene popolata dall'applicazione contenitore (es. internauta) implementando it.bologna.ausl.internauta.utils.parameters.manager.configuration.ParametersManagerConfiguration
    * Contiene la mappa codice-azienda->id-azienda
    */
    @Autowired
    @Qualifier("codiceAziendaIdAziendaMap")
    private Map<String, Integer> codiceAziendaIdAziendaMap;
    
    private List<ParametroAziende> downloaderParams;
    
    /**
     * Questo metodo viene eseguito in fase di boot dell'applicazione.
     * Inizializza il tutto
     * @throws UnknownHostException
     * @throws IOException
     * @throws FirmaRemotaConfigurationException 
     */
    @PostConstruct
    public void init() throws UnknownHostException, IOException, FirmaRemotaConfigurationException {
        List<ParametroAziende> parametersMinIO = parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.minIOConfig.toString());
        if (parametersMinIO == null || parametersMinIO.isEmpty() || parametersMinIO.size() > 1) {
            throw new FirmaRemotaConfigurationException(String.format("il parametro %s non è stato trovato nei parametri_aziende, oppure è presente più volte per la stessa azienda", ParametriAziendeReader.ParametriAzienda.minIOConfig.toString()));
        }
        Map<String, Object> minIOConfig = parametriAziendeReader.getValue(parametersMinIO.get(0), new TypeReference<Map<String, Object>>() {});
        
        this.firmaRemotaConfigurationParams = parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.firmaRemota.toString());
       if (this.firmaRemotaConfigurationParams == null || this.firmaRemotaConfigurationParams.isEmpty()) {
            throw new FirmaRemotaConfigurationException(String.format("parametro %s non presente", ParametriAziendeReader.ParametriAzienda.firmaRemota.toString()));
        }
        
        initMinIO(minIOConfig);
        
        // vengono letti i parametri del downloader per tutte le aziende. Si possono ottenere poi quelli per l'azienda desiderata tramite il metodo getDownloaderParams()
        this.downloaderParams = parametriAziendeReader.getParameters(ParametriAziendeReader.ParametriAzienda.downloader.toString());
        if (this.downloaderParams == null || this.downloaderParams.isEmpty()) {
            throw new FirmaRemotaConfigurationException(String.format("parametro %s non presente", ParametriAziendeReader.ParametriAzienda.downloader.toString()));
        }
    }
    
    /**
     * Torna i parametri Downloader relativi all'azienda passata (uploadUrl e downloadUrl)
     * @param codiceAzienda l'azienda di cui si vogliono conoscere i parametri
     * @return
     */
    public Map<String, String> getDownloaderParams(String codiceAzienda) {
        Integer idAzienda = this.codiceAziendaIdAziendaMap.get(codiceAzienda);
        Optional<ParametroAziende> paramOp = this.downloaderParams.stream().filter(p -> Arrays.stream(p.getIdAziende()).anyMatch(idAzienda::equals)).findFirst();
        ParametroAziende param;
        if (paramOp.isPresent()) {
            param = paramOp.get();
        } else {
            param = this.downloaderParams.get(0);
        }
        Map<String, String> paramValue = this.parametriAziendeReader.getValue(param, new TypeReference<Map<String, String>>(){});
        return paramValue;
    }
    
    /**
     * inizializza la connessione a MinIO
     * @param minIOConfig 
     */
    private void initMinIO(Map<String, Object> minIOConfig) {
        String minIODBDriver = (String) minIOConfig.get("DBDriver");
        String minIODBUrl = (String) minIOConfig.get("DBUrl");
        String minIODBUsername = (String) minIOConfig.get("DBUsername");
        String minIODBPassword = (String) minIOConfig.get("DBPassword");
        Integer maxPoolSize = (Integer) minIOConfig.get("maxPoolSize");
        minIOWrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, maxPoolSize, objectMapper);
    }

    /**
     * Torna l'ogetto per interagire conMinIO
     * @return l'ogetto per interagire conMinIO
     */
    public MinIOWrapper getMinIOWrapper() {
        return minIOWrapper;
    }

    /**
     * Torna la configurazione della firma remota per l'azienda passata
     * @param codiceAzienda
     * @return 
     */
    public Map<String, Map<String, Object>> getFirmaRemotaConfiguration(String codiceAzienda) {
        Integer idAzienda = this.codiceAziendaIdAziendaMap.get(codiceAzienda);
        Optional<ParametroAziende> paramOp = this.firmaRemotaConfigurationParams.stream().filter(p -> Arrays.stream(p.getIdAziende()).anyMatch(idAzienda::equals)).findFirst();
        ParametroAziende param;
        if (paramOp.isPresent()) {
            param = paramOp.get();
        } else {
            param = this.firmaRemotaConfigurationParams.get(0);
        }
        Map<String, Map<String, Object>> paramValue = this.parametriAziendeReader.getValue(param, new TypeReference<Map<String, Map<String, Object>>>(){});
        return paramValue;
    }
}
