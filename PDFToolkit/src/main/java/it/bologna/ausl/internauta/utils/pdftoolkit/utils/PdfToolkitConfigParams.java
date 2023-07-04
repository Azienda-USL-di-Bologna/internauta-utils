package it.bologna.ausl.internauta.utils.pdftoolkit.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import it.bologna.ausl.model.entities.pdftoolkit.Parameter;
import it.bologna.ausl.internauta.utils.pdftoolkit.exceptions.PdfToolkitConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import it.bologna.ausl.internauta.utils.pdftoolkit.repositories.ParameterRepository;
import java.util.HashMap;

/**
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
@Service
public class PdfToolkitConfigParams {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfToolkitConfigParams.class);
    public static final String WORKDIR = System.getProperty("java.io.tmpdir");
    public static final String RESOURCES_RELATIVE_PATH = "/resources/reporter";
    public static final String TEMPLATES_RELATIVE_PATH = "/templates";
     
    public enum ParameterIds {
        downloader,
        minIOConfig
    }
    
    public enum DownloaderParamsKey {
        uploadUrl,
        downloadUrl,
        pdfToolkitBucket
    }
        
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    @Qualifier("pdfToolkitParameterRepository")
    private ParameterRepository parameterRepository;
    
    private MinIOWrapper minIOWrapper;
    private Map<String, Object> downloaderParams;
       
    /**
     * Questo metodo viene eseguito in fase di boot dell'applicazione.
     * Inizializza il tutto
     * @throws UnknownHostException
     * @throws IOException
     * @throws PdfToolkitConfigurationException 
     */
    @PostConstruct
    public void init() throws UnknownHostException, IOException, PdfToolkitConfigurationException {
        
        // lettura dei parametr di MinIO
        Optional<Parameter> minIOParameterOp = parameterRepository.findById(ParameterIds.minIOConfig.toString());
        if (!minIOParameterOp.isPresent() || minIOParameterOp.get().getValue().isEmpty()) {
            throw new PdfToolkitConfigurationException(String.format("il parametro %s non è stato trovato nella tabella pdf_toolkit.parameters", ParameterIds.minIOConfig.toString()));
        }
        Map<String, Object> minIOConfig = minIOParameterOp.get().getValue();
        
        initMinIO(minIOConfig);
        
        // lettura dei parametri del downloader
        Optional<Parameter> downloaderParameterOp = parameterRepository.findById(ParameterIds.downloader.toString());
        if (!downloaderParameterOp.isPresent() || downloaderParameterOp.get().getValue().isEmpty()) {
            throw new PdfToolkitConfigurationException(String.format("il parametro %s non è stato trovato nella tabella pdf_toolkit.parameters", ParameterIds.downloader.toString()));
        }
        // vengono letti i parametri del downloader per tutte le aziende. Si possono ottenere poi quelli per l'azienda desiderata tramite il metodo getDownloaderParams()
        this.downloaderParams = downloaderParameterOp.get().getValue();
        //TODO: Chiedere a GDM come passare il CODICE
        try {
            List<MinIOWrapperFileInfo> filesInPath = minIOWrapper.getFilesInPath(RESOURCES_RELATIVE_PATH, "105");
            for (MinIOWrapperFileInfo minIOFileInfo : filesInPath) {
                try (InputStream fileInputStream = minIOWrapper.getByFileId(minIOFileInfo.getFileId())) {
                    File targetFile = new File(String.format("%s%s%s", WORKDIR, "/", minIOFileInfo.getPath()));

                    java.nio.file.Files.copy(
                        fileInputStream, 
                        targetFile.toPath(), 
                        StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (MinIOWrapperException ex) {
            logger.error("Errore durante il download dei resource files da minIO");
//            throw new PdfToolkitConfigurationException(String.format("Errore durante il download dei resource files da minIO"));
        }
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
     * Torna il nome del bucket del pdfToolkitBucket
     * @return il nome del bucket del pdfToolkitBucket
     */
    public String getPdfToolkitBucket() {
        return (String)this.downloaderParams.get(DownloaderParamsKey.pdfToolkitBucket.toString());
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

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
