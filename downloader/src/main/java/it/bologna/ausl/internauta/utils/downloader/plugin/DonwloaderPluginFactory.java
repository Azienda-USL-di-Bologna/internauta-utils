package it.bologna.ausl.internauta.utils.downloader.plugin;

import it.bologna.ausl.internauta.utils.downloader.configuration.RepositoryManager;
import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderPluginException;
import it.bologna.ausl.internauta.utils.downloader.plugin.impl.MinIODownloader;
import it.bologna.ausl.internauta.utils.downloader.plugin.impl.MongoDownloader;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author gdm
 */
public class DonwloaderPluginFactory {
    private static Logger logger = LoggerFactory.getLogger(DonwloaderPluginFactory.class);
    
    public static  enum Source {
        MinIO, Mongo
    }
    
    // mappa per ogni source l'implementazione del downloader plugin corrispondente
    private static final Map<Source, Class<? extends DownloaderPlugin>> pluginMap = new HashMap();
    static { // in questo modo la mappa viene riempita all'avvio dell'applicazione
        pluginMap.put(Source.MinIO, MinIODownloader.class);
        pluginMap.put(Source.Mongo, MongoDownloader.class);
    }
    
    /**
     * Costruisce tramite reflection il corretto downloader plugin basandosi sul parametro source e settandogli i parametri
     * @param source
     * @param params
     * @param repositoryManager
     * @return
     * @throws DownloaderPluginException 
     */
    public static DownloaderPlugin getDownloaderPlugin(Source source, Map<String, Object> params, RepositoryManager repositoryManager) throws DownloaderPluginException {
        try {
//            String currPackage = DonwloaderPluginFactory.class.getPackage().getName();
            // reperisco la classe che implementa il downloader plugin corretto in base al source
            Class<? extends DownloaderPlugin> downloaderPluginClass = pluginMap.get(source);
            
            // tramite reflection reperisco il cotruttore e ne creo un'istanza
            DownloaderPlugin downloaderPluginInstance = downloaderPluginClass.getConstructor(Map.class, RepositoryManager.class).newInstance(params, repositoryManager);
            return downloaderPluginInstance;
        } catch (Exception ex) {
            String errorMessage = String.format("errore nell'instanziare il plugin con source %s", source.toString());
            logger.error(errorMessage, ex);
            throw new DownloaderPluginException(errorMessage, ex);
        }
    }
}
