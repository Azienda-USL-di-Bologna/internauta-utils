package it.bologna.ausl.internauta.utils.downloader.plugin.impl;

import it.bologna.ausl.internauta.utils.downloader.configuration.RepositoryManager;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderDownloadException;
import it.bologna.ausl.internauta.utils.downloader.plugin.DownloaderDownloadPlugin;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.MinIOWrapperFileInfo;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementa il plugin per lo scaricamento dei file da MinIO
 * @author gdm
 */
public class MinIODownloader extends DownloaderDownloadPlugin {
    
    private static Logger logger = LoggerFactory.getLogger(MinIODownloader.class);

    public MinIODownloader(Map<String, Object> params, RepositoryManager repositoryManager) {
        super(params, repositoryManager);
    }
    
    /**
     * Legge il fileId dal campo fileId dei params e ne torna lo stream
     * @return lo stream del file identificato dal fileId passato nel campo fileId dei params
     * @throws DownloaderDownloadException se c'è un errore nel reperimento del file
     */
    @Override
    public InputStream getFile() throws DownloaderDownloadException {
        
        String fileId = (String) params.get("fileId");
        
        // reperisco la connessione a MinIO dal repositoryManager tramite il metodo opportuno
        MinIOWrapper minIOWrapper = super.repositoryManager.getMinIOWrapper();
        try {
            return  minIOWrapper.getByFileId(fileId);
        } catch (Exception ex) {
            String errorMessage = "errore nello scaricamento del file";
            logger.error(String.format(errorMessage + " con id %s", fileId));
            throw new DownloaderDownloadException(errorMessage, ex);
        }
    }
    
    /**
     * Legge il fileId dal campo fileId dei params e lo usa per reperire il nome del file
     * @return il nome del file identificato dal fileId passato nel campo fileId dei params
     * @throws DownloaderDownloadException se c'è un errore nel reperimento del file
     */
    @Override
    public String getFileName() throws DownloaderDownloadException {
       String fileId = (String) params.get("fileId");
       
       // reperisco la connessione a MinIO dal repositoryManager tramite il metodo opportuno
       MinIOWrapper minIOWrapper = super.repositoryManager.getMinIOWrapper();
       try {
           MinIOWrapperFileInfo fileInfo = minIOWrapper.getFileInfoByFileId(fileId);
           return fileInfo.getFileName();
        } catch (Exception ex) {
            String errorMessage = "errore nel reperimento del nome del file";
            logger.error(String.format(errorMessage + " con id %s", fileId));
            throw new DownloaderDownloadException(errorMessage, ex);
        }
       
    }
}
