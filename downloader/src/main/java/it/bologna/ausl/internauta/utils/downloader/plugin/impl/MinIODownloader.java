package it.bologna.ausl.internauta.utils.downloader.plugin.impl;

import it.bologna.ausl.internauta.utils.downloader.plugin.DownloaderPlugin;
import java.io.InputStream;
import java.util.Map;

/**
 *
 * @author gdm
 */
public class MinIODownloader extends DownloaderPlugin {

    public MinIODownloader(Map<String, Object> params) {
        super(params);
    }
    
    @Override
    public InputStream getFile() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
