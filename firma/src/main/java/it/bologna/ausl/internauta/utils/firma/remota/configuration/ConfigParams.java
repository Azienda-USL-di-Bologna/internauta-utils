package it.bologna.ausl.internauta.utils.firma.remota.configuration;



import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.repositories.ParameterRepository;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.firma.Parameter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.stereotype.Service;

/**
 * Questa classe legge, tramite il modulo parameters-manager, i parametri di configurazione dal database (tabella configurazione.parametri_azienda)
 * 
 * @author gdm
 */
@Service
public class ConfigParams {

    private static Logger logger = LoggerFactory.getLogger(ConfigParams.class);

    public enum ParameterIds {
        downloader,
        minIOConfig,
    }
    
    public enum DownloaderParamsKey {
        uploadUrl,
        downloadUrl,
        uploaderBucket
    }
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ParameterRepository parameterRepository;
    
    private MinIOWrapper minIOWrapper;
        
    private Map<String, Object> downloaderParams;
       
    /**
     * Questo metodo viene eseguito in fase di boot dell'applicazione.
     * Inizializza il tutto
     * @throws UnknownHostException
     * @throws IOException
     * @throws FirmaRemotaConfigurationException 
     */
    @PostConstruct
    public void init() throws UnknownHostException, IOException, FirmaRemotaConfigurationException {
        
        // lettura dei parametr di MinIO
        Optional<Parameter> minIOParameterOp = parameterRepository.findById(ParameterIds.minIOConfig.toString());
        if (!minIOParameterOp.isPresent() || minIOParameterOp.get().getValue().isEmpty()) {
            throw new FirmaRemotaConfigurationException(String.format("il parametro %s non è stato trovato nella tabella firma.parameters", ParameterIds.minIOConfig.toString()));
        }
        Map<String, Object> minIOConfig = minIOParameterOp.get().getValue();
        
        initMinIO(minIOConfig);
        
        // lettura dei parametri del downloader
        Optional<Parameter> downloaderParameterOp = parameterRepository.findById(ParameterIds.downloader.toString());
        if (!downloaderParameterOp.isPresent() || downloaderParameterOp.get().getValue().isEmpty()) {
            throw new FirmaRemotaConfigurationException(String.format("il parametro %s non è stato trovato nella tabella firma.parameters", ParameterIds.downloader.toString()));
        }
        
        // vengono letti i parametri del downloader per tutte le aziende. Si possono ottenere poi quelli per l'azienda desiderata tramite il metodo getDownloaderParams()
        this.downloaderParams = downloaderParameterOp.get().getValue();
    }
    
    /**
     * Torna l'url del downloder sostituendo ai segnaposto (se ci sono) lo schema, l'hostname e la porta passati
     * @param scheme schema dell'url chiamante (es: http, https)
     * @param hostname hostname dell'url chiamante (es. localhost, gdml.inetrnal.ausl.bologna.it, ecc)
     * @param port la porta da sostituire
     * @return
     */
    public String getDownloaderUrl(String scheme, String hostname, Integer port) {
        return ((String)this.downloaderParams.get(DownloaderParamsKey.downloadUrl.toString()))
                .replace("{scheme}", scheme)
                .replace("{hostname}", hostname)
                .replace("{port}", port.toString());
    }
    
    /**
     * Torna l'url dell'uploader sostituendo ai segnaposto (se ci sono) lo schema, l'hostname e la porta passati
     * @param scheme schema dell'url chiamante (es: http, https)
     * @param hostname hostname dell'url chiamante (es. localhost, gdml.inetrnal.ausl.bologna.it, ecc)
     * @param port la porta da sostituire
     * @return
     */
    public String getUploaderUrl(String scheme, String hostname, Integer port) {
        return ((String)this.downloaderParams.get(DownloaderParamsKey.uploadUrl.toString()))
                .replace("{scheme}", scheme)
                .replace("{hostname}", hostname)
                .replace("{port}", port.toString());
    }
    
    /**
     * Torna il nome del bucket dell'uploader
     * @return il nome del bucket dell'uploader
     */
    public String getUploaderUrlBucket() {
        return ((String)this.downloaderParams.get(DownloaderParamsKey.uploaderBucket.toString()));
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
    
}
