package it.bologna.ausl.internauta.utils.downloader.plugin;

import it.bologna.ausl.internauta.utils.downloader.exceptions.DownloaderPluginException;
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
    
    
    public static DownloaderPlugin getDownloaderPlugin(Source source, Map<String, Object> params) throws DownloaderPluginException {
        try {
            String currPackage = DonwloaderPluginFactory.class.getPackage().getName();
            Class<DownloaderPlugin> downloaderPluginClass = (Class<DownloaderPlugin>) Class.forName(currPackage + ".impl." + source.toString());
            DownloaderPlugin downloaderPlugin = downloaderPluginClass.getConstructor().newInstance(params);
            return downloaderPlugin;
        } catch (Exception ex) {
            String errorMessage = String.format("errore nell'instanziare il plugin con source %s", source.toString());
            logger.error(errorMessage);
            throw new DownloaderPluginException(errorMessage, ex);
        }
    }
}
