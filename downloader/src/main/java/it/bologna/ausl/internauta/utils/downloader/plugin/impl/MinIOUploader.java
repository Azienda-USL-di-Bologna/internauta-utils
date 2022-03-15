package it.bologna.ausl.internauta.utils.downloader.plugin.impl;

import it.bologna.ausl.internauta.utils.downloader.configuration.RepositoryManager;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderUploadException;
import it.bologna.ausl.internauta.utils.downloader.plugin.DownloaderUploadPlugin;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Implementa il plugin per lo scaricamento dei file da MinIO
 * @author gdm
 */
public class MinIOUploader extends DownloaderUploadPlugin {
    
    private static Logger logger = LoggerFactory.getLogger(MinIOUploader.class);

    public MinIOUploader(Map<String, Object> params, RepositoryManager repositoryManager) {
        super(params, repositoryManager);
    }
    
    /**
     * Carica il file su MinIO con le informazioni passate nei params
     * Se il bucket non è passato sarà uguale al codiceAzienda.
     * Se overwrite non è passato sarà considerato false.
     * @param file lo stream del file da caricare su MinIO
     * @param path il path logico del file su MinIO
     * @param fileName il nome del file logico su MinIO
     * @return i params per la creazione del context per poterlo scaricare
     * @throws DownloaderUploadException se c'è un errore nell'upload del file del file
     */
    @Override
    public Map<String, Object> putFile(InputStream file, String path, String fileName) throws DownloaderUploadException {
        if (params == null || params.isEmpty()) {
            throw new DownloaderUploadException("i params sono nulli o vuoti");
        }
        
        // il codiceAzienda è obbligatorio, se non c'è lancio eccezione
        if (!params.containsKey("codiceAzienda")) {
            throw new DownloaderUploadException("il parametro codiceAzienda non è stato passato");
        }
        String codiceAzienda = (String) params.get("codiceAzienda");
        
        Map<String, Object> metadata = null;
        if (params.containsKey("metadata")) {
            metadata = (Map<String, Object>) params.get("metadata");
        }

        // se non specifico il bucket allora lo rendo uguiale al codiceAzienda
        String bucket;
        if (params.containsKey("bucket")) {
            bucket = (String) params.get("bucket");
        } else {
            bucket = codiceAzienda;
        }
        
        // se non c'è overwrite allora lo considero false
        Boolean overwrite;
        if (params.containsKey("overwrite")) {
            overwrite = (Boolean) params.get("overwrite");
        } else {
            overwrite = false;
        }
        
        // mi assicuro che mi sia stato passato un path, altrimenti lancio eccezione
        if (!StringUtils.hasText(path)) {
            String errorMessage = "il parametro path non è stato passato";
            logger.error(errorMessage);
            throw new DownloaderUploadException(errorMessage);
        }
        
        // mi assicuro che mi sia stato passato un fileName, altrimenti lancio eccezione
        if (!StringUtils.hasText(fileName)) {
            String errorMessage = "il parametro fileName non è stato passato";
            logger.error(errorMessage);
            throw new DownloaderUploadException(errorMessage);
        }
        
        // reperisco la connessione a MinIO dal repositoryManager tramite il metodo opportuno
        MinIOWrapper minIOWrapper = super.repositoryManager.getMinIOWrapper();
        try {
            // carico il file su minIO, generando anche un uuidMongo tramite UUID.randomUUID().toString(), in modo che il file sia trovabile anche con la libreria RepositoryWrapper
            MinIOWrapperFileInfo res = minIOWrapper.putWithBucket(file, codiceAzienda, path, fileName, metadata, overwrite, UUID.randomUUID().toString(), bucket);
            
            // Come risultato torno i params che servono per il DownloadPlugin con source MinIO
            Map<String, Object> resParams = new HashMap();
            resParams.put("fileId", res.getFileId());
            return resParams;
        } catch (Exception ex) {
            String errorMessage = "errore nell'upload del file";
            logger.error(errorMessage, ex);
            throw new DownloaderUploadException(errorMessage, ex);
        }
    }
}
