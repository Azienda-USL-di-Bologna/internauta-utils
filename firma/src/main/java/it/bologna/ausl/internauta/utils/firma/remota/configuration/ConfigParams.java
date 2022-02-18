package it.bologna.ausl.internauta.utils.firma.remota.configuration;



import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.firma.remota.data.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.parameters.manager.ParametriAziendeReader;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.configurazione.ParametroAziende;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

/**
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

    Map<String, Map<String, Object>> firmaRemotaConfiguration;
    private MinIOWrapper minIOWrapper;
    
    @PostConstruct
    public void init() throws UnknownHostException, IOException, FirmaRemotaConfigurationException {
        List<ParametroAziende> parameters = parametriAziendeReader.getParameters(
                ParametriAziendeReader.ParametriAzienda.minIOConfig.toString());
        if (parameters == null || parameters.isEmpty() || parameters.size() > 1) {
            throw new FirmaRemotaConfigurationException(String.format("il parametro %s non è stato trovato nei parametri_aziende, oppure è presente più volte per la stessa azienda", ParametriAziendeReader.ParametriAzienda.minIOConfig.toString()));
        }
        Map<String, Object> minIOConfig = parametriAziendeReader.getValue(parameters.get(0), new TypeReference<Map<String, Object>>() {});
        
        this.firmaRemotaConfiguration = parametriAziendeReader.getValue(parameters.get(0), new TypeReference<Map<String, Map<String, Object>>>() {});

        initMinIO(minIOConfig);
    }
    
    private void initMinIO(Map<String, Object> minIOConfig) {
        String minIODBDriver = (String) minIOConfig.get("DBDriver");
        String minIODBUrl = (String) minIOConfig.get("DBUrl");
        String minIODBUsername = (String) minIOConfig.get("DBUsername");
        String minIODBPassword = (String) minIOConfig.get("DBPassword");
        Integer maxPoolSize = (Integer) minIOConfig.get("maxPoolSize");
        minIOWrapper = new MinIOWrapper(minIODBDriver, minIODBUrl, minIODBUsername, minIODBPassword, maxPoolSize, objectMapper);
    }

    public MinIOWrapper getMinIOWrapper() {
        return minIOWrapper;
    }

    public Map<String, Map<String, Object>> getFirmaRemotaConfiguration() {
        return firmaRemotaConfiguration;
    }
}
