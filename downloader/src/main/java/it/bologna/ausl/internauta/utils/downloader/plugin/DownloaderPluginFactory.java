package it.bologna.ausl.internauta.utils.downloader.plugin;

import it.bologna.ausl.internauta.utils.downloader.configuration.RepositoryManager;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderPluginException;
import it.bologna.ausl.internauta.utils.downloader.plugin.impl.DefaultUploader;
import it.bologna.ausl.internauta.utils.downloader.plugin.impl.MinIODownloader;
import it.bologna.ausl.internauta.utils.downloader.plugin.impl.MinIOUploader;
import it.bologna.ausl.internauta.utils.downloader.plugin.impl.MongoDownloader;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class DownloaderPluginFactory {
    private static Logger logger = LoggerFactory.getLogger(DownloaderPluginFactory.class);
    
    public static  enum TargetRepository {
        MinIO, Mongo, Default
    }
    
    // mappa per ogni source l'implementazione del downloader plugin corrispondente
    private static final Map<TargetRepository, Class<? extends DownloaderDownloadPlugin>> downloadPluginMap = new HashMap();
    private static final Map<TargetRepository, Class<? extends DownloaderUploadPlugin>> uploadPluginMap = new HashMap();
    static { // in questo modo la mappa viene riempita all'avvio dell'applicazione
        downloadPluginMap.put(TargetRepository.MinIO, MinIODownloader.class);
        downloadPluginMap.put(TargetRepository.Default, MinIODownloader.class);
        downloadPluginMap.put(TargetRepository.Mongo, MongoDownloader.class);
        
        uploadPluginMap.put(TargetRepository.MinIO, MinIOUploader.class);
        uploadPluginMap.put(TargetRepository.Default, DefaultUploader.class);
    }
    
    /**
     * Costruisce tramite reflection il corretto download plugin basandosi sul parametro source e settandogli i parametri
     * @param source il source (source) letto dal claim context
     * @param params i parametri (params) letti dal claim context
     * @param repositoryManager il RepositoryManager ottenuto in autowired (che viene configurato in avvio del'applicazione)
     * @return il corretto download plugin
     * @throws DownloaderPluginException 
     */
    public static DownloaderDownloadPlugin getDownloadPlugin(TargetRepository source, Map<String, Object> params, RepositoryManager repositoryManager) throws DownloaderPluginException {
        try {
//            String currPackage = DonwloaderPluginFactory.class.getPackage().getName();
            // reperisco la classe che implementa il downloader plugin corretto in base al source
            Class<? extends DownloaderDownloadPlugin> downloadPluginClass = downloadPluginMap.get(source);
            
            // tramite reflection reperisco il cotruttore e ne creo un'istanza
            DownloaderDownloadPlugin downloadPluginInstance = downloadPluginClass.getConstructor(Map.class, RepositoryManager.class).newInstance(params, repositoryManager);
            return downloadPluginInstance;
        } catch (Exception ex) {
            String errorMessage = String.format("errore nell'instanziare il plugin con source %s", source.toString());
            logger.error(errorMessage, ex);
            throw new DownloaderPluginException(errorMessage, ex);
        }
    }
    
    /**
     * Costruisce tramite reflection il corretto upload plugin basandosi sul parametro target e settandogli i parametri
     * @param target il target (target) letto dal claim context
     * @param params i parametri (params) letti dal claim context
     * @param repositoryManager il RepositoryManager ottenuto in autowired (che viene configurato in avvio del'applicazione)
     * @return il corretto upload plugin
     * @throws DownloaderPluginException 
     */
    public static DownloaderUploadPlugin getUploadPlugin(TargetRepository target, Map<String, Object> params, RepositoryManager repositoryManager) throws DownloaderPluginException {
        try {
            Class<? extends DownloaderUploadPlugin> uploadPluginClass = uploadPluginMap.get(target);
            
            // tramite reflection reperisco il cotruttore e ne creo un'istanza
            DownloaderUploadPlugin uploadPluginInstance = uploadPluginClass.getConstructor(Map.class, RepositoryManager.class).newInstance(params, repositoryManager);
            return uploadPluginInstance;
        } catch (Exception ex) {
            String errorMessage = String.format("errore nell'instanziare il plugin con target %s", target.toString());
            logger.error(errorMessage, ex);
            throw new DownloaderPluginException(errorMessage, ex);
        }
    }
}
