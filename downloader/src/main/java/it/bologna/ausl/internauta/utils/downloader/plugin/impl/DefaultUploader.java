package it.bologna.ausl.internauta.utils.downloader.plugin.impl;

import it.bologna.ausl.internauta.utils.downloader.configuration.RepositoryManager;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderUploadException;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementa il plugin di default per l'upload. I file vengono caricati su MinIO in um path fisso(costante DEFAULT_PATH) e con nome file passato.
 * Se il nome file non viene passato, viene usato il nome del file del client cliamante (letto dalla request http)
 * @author gdm
 */
public class DefaultUploader extends MinIOUploader {
    
    private static Logger logger = LoggerFactory.getLogger(DefaultUploader.class);

    private final String BUCKET = "uploader";
    
    private final String DEFAULT_PATH = "/uploader";;
    
    public DefaultUploader(Map<String, Object> params, RepositoryManager repositoryManager) {
        super(params, repositoryManager);
    }
    
    /**
     * Carica il file sul repository di default (MinIO) con le informazioni passate nei params
     * il bucket usato è un bucket apposito per l'uploader (costante BUCKET)
     * L'unico params passabile è "metadata".
     * @param file lo stream del file da caricare sul repository
     * @param path non viene usato, viene generato a caso
     * @param fileName il nome del file logico da inserire (andrebbe passato quello reperito dalla request)
     * @return i params per la creazione del context per poterlo scaricare
     * @throws DownloaderUploadException se c'è un errore nell'upload del file del file
     */
    @Override
    public Map<String, Object> putFile(InputStream file, String path, String fileName) throws DownloaderUploadException {
        
        Map<String, Object> metadata = null;
        if (params != null && params.containsKey("metadata")) {
            metadata = (Map<String, Object>) params.get("metadata");
        }

        String bucket = BUCKET;
        String codiceAzienda = BUCKET;
        
        Boolean overwrite = false;
        
        // reperisco la connessione a MinIO dal repositoryManager tramite il metodo opportuno
        MinIOWrapper minIOWrapper = super.repositoryManager.getMinIOWrapper();
        try {
            
            // carico il file su minIO, generando anche un uuidMongo tramite UUID.randomUUID().toString(), in modo che il file sia trovabile anche con la libreria RepositoryWrapper
            MinIOWrapperFileInfo res = minIOWrapper.put(file, codiceAzienda, DEFAULT_PATH, fileName, metadata, overwrite, UUID.randomUUID().toString(), bucket);
            
            // Come risultato torno i params che servono per il DownloadPlugin con source Default (o MinIO che per il momento sono uguali)
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
