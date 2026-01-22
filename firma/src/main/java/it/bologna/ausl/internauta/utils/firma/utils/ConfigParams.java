package it.bologna.ausl.internauta.utils.firma.utils;



import it.bologna.ausl.internauta.utils.firma.exceptions.FirmaParameterException;
import it.bologna.ausl.internauta.utils.firma.repositories.ParameterRepository;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.model.entities.firma.Parameter;
import it.bologna.ausl.model.entities.firma.QParameter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Questa classe legge, i parametri di configurazione dal database (tabella firma.parameters)
 * 
 * @author gdm
 */
@Service
public class ConfigParams {

    private static Logger logger = LoggerFactory.getLogger(ConfigParams.class);

    public enum ParameterIds {
        downloader,
        minIOConfig,
        externalCheckCertificate,
        externalSignAndCertificateValidator,
        firmaJnJRequestParameter
    }
    
    public enum DownloaderParamsKey {
        uploadUrl,
        downloadUrl,
        uploaderBucket
    }
    
    public enum ExternalCheckCertificateParamsKey {
        url
    }
    
    public enum ExternalSignAndCertificateValidatorParamsKey {
        validateDocumentUrl,
        validateCertificateUrl
    }
    
    public enum FirmaJnJRequestParameterParamsParamsKey {
        hourBeforeDelete
    }
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ParameterRepository parameterRepository;
    
    private MinIOWrapper minIOWrapper;
        
    private Map<String, Object> downloaderParams;
    private Map<String, Object> externalCheckCertificateParams;
    private Map<String, Object> externalSignAndCertificateValidatorParams;
    private Map<String, Object> firmaJnJRequestParameterParams;
       
    /**
     * Questo metodo viene eseguito in fase di boot dell'applicazione.
     * Inizializza il tutto
     * @throws UnknownHostException
     * @throws IOException
     * @throws it.bologna.ausl.internauta.utils.firma.exceptions.FirmaParameterException
     */
    @PostConstruct
    public void init() throws UnknownHostException, IOException, FirmaParameterException {
        
        // lettura dei parametr di MinIO
        Parameter minIOParameter = getParameter(ParameterIds.minIOConfig);
        Map<String, Object> minIOConfig = minIOParameter.getValue();
        
        initMinIO(minIOConfig);
        
        // lettura dei parametri del downloader
        Parameter downloaderParameter = getParameter(ParameterIds.downloader);

        // vengono letti i parametri del downloader per tutte le aziende. Si possono ottenere poi quelli per l'azienda desiderata tramite il metodo getDownloaderParams()
        this.downloaderParams = downloaderParameter.getValue();
        
        // lettura del parametro externalCheckCertificate
        Parameter externalCheckCertificate = getParameter(ParameterIds.externalCheckCertificate);
        this.externalCheckCertificateParams = externalCheckCertificate.getValue();
        
        // lettura del parametro del nuovo validatore externalSignAndCertificateValidator
        Parameter externalSignAndCertificateValidator = getParameter(ParameterIds.externalSignAndCertificateValidator);
        this.externalSignAndCertificateValidatorParams = externalSignAndCertificateValidator.getValue();
        
        // lettura del parametro del nuovo validatore externalSignAndCertificateValidator
        Parameter firmaJnJRequestParameter = getParameter(ParameterIds.firmaJnJRequestParameter);
        this.firmaJnJRequestParameterParams = firmaJnJRequestParameter.getValue();
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
     * Torna l'url del vecchio servizio esterno di controllo del certificato
     * @param scheme schema dell'url chiamante (es: http, https)
     * @param hostname hostname dell'url chiamante (es. localhost, gdml.inetrnal.ausl.bologna.it, ecc)
     * @param port la porta da sostituire
     * @return l'url del servizio esterno di controllo del certificato
     */
    public String getExternalCheckCertificateUrl(String scheme, String hostname, Integer port) {
        return ((String) this.externalCheckCertificateParams.get(ExternalCheckCertificateParamsKey.url.toString()))
            .replace("{scheme}", scheme)
            .replace("{hostname}", hostname)
            .replace("{port}", port.toString());
    }
    
    /**
     * Torna il parametro richiesto del servizio esterno di controllo dei file firmati e dei certificati
     * @param key la chiave del parametro che si vuole ottenere
     * @param scheme schema dell'url chiamante (es: http, https)
     * @param hostname hostname dell'url chiamante (es. localhost, gdml.inetrnal.ausl.bologna.it, ecc)
     * @param port la porta da sostituire
     * @return il parametro richiesto del servizio esterno di controllo dei file firmati e dei certificati
     */
//    public String getExternalSignAndCertificateValidator(ExternalSignAndCertificateValidatorParamsKey key, String scheme, String hostname, Integer port) {
//        return ((String) this.externalSignAndCertificateValidatorParams.get(key.toString()))
//            .replace("{scheme}", scheme)
////            .replace("{hostname}", "localhost")
//            .replace("{hostname}", hostname)
////            .replace("{port}", "10008");
//            .replace("{port}", port.toString());
//    }
    public String getExternalSignAndCertificateValidator(ExternalSignAndCertificateValidatorParamsKey key) {
        return (String) this.externalSignAndCertificateValidatorParams.get(key.toString());
    }
    
//    public String getExternalSignAndCertificateValidator(ExternalSignAndCertificateValidatorParamsKey key, String scheme, String hostname, Integer port) {
//        return ((String) this.externalSignAndCertificateValidatorParams.get(key.toString()))
//            .replace("{scheme}", scheme)
////            .replace("{hostname}", "localhost")
//            .replace("{hostname}", hostname)
////            .replace("{port}", "10008");
//            .replace("{port}", port.toString());
//    }
    
    /**
     * Torna l'url del nuovo servizio esterno di controllo di un file firmato

     * @return l'url del servizio esterno di controllo del certificato
     */
    public String getExternalSignAndCertificateValidatorValidateDocumentUrl() {
        return (String) this.externalSignAndCertificateValidatorParams.get(ExternalSignAndCertificateValidatorParamsKey.validateDocumentUrl.toString());
                
    }
    
    

    /**
     * Torna l'url del nuovo servizio esterno di controllo del certificato
     * @param scheme schema dell'url chiamante (es: http, https)
     * @param hostname hostname dell'url chiamante (es. localhost, gdml.inetrnal.ausl.bologna.it, ecc)
     * @param port la porta da sostituire
     * @return l'url del servizio esterno di controllo del certificato
     */
    public String getExternalSignAndCertificateValidatoValidateCertificateUrl(String scheme, String hostname, Integer port) {
        return ((String) this.externalSignAndCertificateValidatorParams.get(ExternalSignAndCertificateValidatorParamsKey.validateCertificateUrl.toString()))
                .replace("{scheme}", scheme)
                .replace("{hostname}", hostname)
                .replace("{port}", port.toString());
    }
    
     public String getExternalSignAndCertificateValidatoValidateCertificateUrl() {
        return ((String) this.externalSignAndCertificateValidatorParams.get(ExternalSignAndCertificateValidatorParamsKey.validateCertificateUrl.toString()));
            
    }
    
    public Map<String, Object> getFirmaJnJRequestParameter() {
        return this.firmaJnJRequestParameterParams;
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

    
    private Parameter getParameter(ParameterIds parameterId) throws FirmaParameterException {
        Optional<Parameter> paramterOp = parameterRepository.findById(parameterId.toString());
        if (paramterOp.isPresent() &&  !paramterOp.get().getValue().isEmpty()) {
            return paramterOp.get();
        } else {
            throw new FirmaParameterException(String.format("il parametro %s non è stato trovato nella tabella firma.parameters", parameterId.toString()));
        }
    }
    /**
     * Torna l'ogetto per interagire conMinIO
     * @return l'ogetto per interagire conMinIO
     */
    public MinIOWrapper getMinIOWrapper() {
        return minIOWrapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
}
