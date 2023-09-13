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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import it.bologna.ausl.internauta.utils.pdftoolkit.repositories.ParameterRepository;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Classe per la configurazione iniziare del modulo. 
 * Legge i parametri dal DB e inizializza gli oggetti necessari del reporter.
 * 
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
@Service
public class PdfToolkitConfigParams {
    
    private static final Logger log = LoggerFactory.getLogger(PdfToolkitConfigParams.class);
    public static final String WORKDIR = System.getProperty("java.io.tmpdir");
    public static final String RESOURCES_RELATIVE_PATH = "/resources/reporter";
    public static final String INTERNAUTA_RELATIVE_PATH = "/internauta";
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
     * Metodo di inizializzazione chiamato dopo la creazione del bean.
     * Effettua la lettura dei parametri di configurazione da MinIO e del downloader,
     * quindi scarica tutti i file di risorse per il modulo PDFToolkit.
     * 
     * @throws UnknownHostException se si verifica un errore durante la risoluzione dell'host
     * @throws IOException se si verifica un errore di input/output
     * @throws PdfToolkitConfigurationException se si verifica un errore di configurazione del PdfToolkit
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
        log.info("Downloading all resource files for PDFToolkit module...");
//        try {
//            List<MinIOWrapperFileInfo> filesInPath = minIOWrapper.getFilesInPath(RESOURCES_RELATIVE_PATH, getPdfToolkitBucket());
//            if (filesInPath != null && !filesInPath.isEmpty()) {
//                log.info("Found {} files in the resources path.", filesInPath.size());
//                for (MinIOWrapperFileInfo minIOFileInfo : filesInPath) {
//                    File targetFile = new File(String.format("%s%s%s", WORKDIR, "/", minIOFileInfo.getPath()));
//                    if (targetFile.exists())
//                        log.debug("The file '{}' already exists, download skipped.", minIOFileInfo.getPath());
//                    else {
//                        log.debug("Downloading file '{}'...", minIOFileInfo.getPath());
//                        try (InputStream fileInputStream = minIOWrapper.getByFileId(minIOFileInfo.getFileId())) {
//                            String destination = targetFile.getPath().replace(minIOFileInfo.getFileName(), "");
//                            if (Files.notExists(Paths.get(destination)))
//                                Files.createDirectories(Paths.get(destination));
//                            java.nio.file.Files.copy(
//                                fileInputStream, 
//                                targetFile.toPath(), 
//                                StandardCopyOption.REPLACE_EXISTING);
//                            log.debug("File '{}' downloaded successfully.", minIOFileInfo.getPath());
//                        } catch (IOException ex) {
//                            log.error("Error occurred during the download of file '{}' from MinIO", minIOFileInfo.getPath(), ex);
//                        }
//                    }
//                }  
//                log.info("All resource files downloaded successfully.");
//            } else {
//                log.info("There aren't any files to download.");
//            }
//        } catch (MinIOWrapperException ex) {
//            log.error("Error occurred in MinIOWrapper when trying to download the files.", ex);
////        throw new PdfToolkitConfigurationException("Error occurred in MinIOWrapper", ex);
//        }
    }
    
    /**
     * Restituisce l'URL di download del downloader.
     * @return L'URL di download come una stringa.
     */
    public String getDownloaderUrl() {
        return ((String)this.downloaderParams.get(DownloaderParamsKey.downloadUrl.toString()));
    }
    
    /**
     * Restituisce l'URL di upload del downloader.
     * @return L'URL di upload come una stringa.
     */
    public String getUploaderUrl() {
        return ((String)this.downloaderParams.get(DownloaderParamsKey.uploadUrl.toString()));
    }
    
    /**
     * Restituisce il nome del bucket del pdfToolkitBucket
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
