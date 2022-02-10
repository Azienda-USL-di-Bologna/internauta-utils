package it.bologna.ausl.internauta.utils.downloader.plugin.impl;

import it.bologna.ausl.internauta.utils.downloader.configuration.RepositoryManager;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderDownloadException;
import it.bologna.ausl.internauta.utils.downloader.plugin.DownloaderPlugin;
import it.bologna.ausl.mongowrapper.MongoWrapper;
import java.io.InputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementa il plugin per lo scaricamento dei file da Mongo.
 * NB: E' probabile che la connessione tramite la libreria MongoWrapper, reperisca i file da MinIO se si sta utilizzando la modalità Mongo/MinIO
 * Nel momento in cui tutti i file saranno spostati su minIO questo plugin non servirà più.
 * 
 * @author gdm
 */
public class MongoDownloader extends DownloaderPlugin {
    private static Logger logger = LoggerFactory.getLogger(DownloaderPlugin.class);
    
    public MongoDownloader(Map<String, Object> params, RepositoryManager repositoryManager) {
        super(params, repositoryManager);
    }
    
    /**
     * Legge l'uuid del file dal campo uuid dei params e ne torna lo stream
     * @return lo stream del file identificato dall'uuid passato nel campo uuid dei params
     * @throws DownloaderDownloadException se c'è un errore nel reperimento del file
     */
    @Override
    public InputStream getFile() throws DownloaderDownloadException {
        String uuid = (String) params.get("uuid");
        String codiceAzienda = (String) params.get("codiceAzienda");
        
        // reperisco la connessione a Mongo/MinIO dal repositoryManager tramite il metodo opportuno
        MongoWrapper mongoWrapper = super.repositoryManager.getMongoWrapper(codiceAzienda);
        try {
            return  mongoWrapper.get(uuid);
        } catch (Exception ex) {
            String errorMessage = "errore nello scaricamento del file";
            logger.error(String.format(errorMessage + " con uuid %s", uuid));
            throw new DownloaderDownloadException(errorMessage, ex);
        }    
    }
    
    /**
     * Legge l'uuid dal campo uuid dei params e lo usa per reperire il nome del file
     * @return il nome del file identificato dall'uuid passato nel campo uuid dei params
     * @throws DownloaderDownloadException se c'è un errore nel reperimento del file
     */
    @Override
    public String getFileName() throws DownloaderDownloadException {
        String uuid = (String) params.get("uuid");
        String codiceAzienda = (String) params.get("codiceAzienda");
        
        // reperisco la connessione a Mongo/MinIO dal repositoryManager tramite il metodo opportuno
        MongoWrapper mongoWrapper = super.repositoryManager.getMongoWrapper(codiceAzienda);
        try {
            return mongoWrapper.getFileName(uuid);
        } catch (Exception ex) {
            String errorMessage = "errore nel reperimento del nome del file";
            logger.error(String.format(errorMessage + " con uuid %s", uuid));
            throw new DownloaderDownloadException(errorMessage, ex);
        }    
    }
    
}
