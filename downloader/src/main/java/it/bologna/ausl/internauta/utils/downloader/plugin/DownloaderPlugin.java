package it.bologna.ausl.internauta.utils.downloader.plugin;

import java.io.InputStream;
import java.util.Map;

/**
 *
 * @author gdm
 */
public abstract class DownloaderPlugin {

    protected Map<String, Object> params;
    
    protected DownloaderPlugin(Map<String, Object> params) {
        this.params = params;
    }

    public abstract InputStream getFile();
}
