package it.bologna.ausl.internauta.utils.downloader.exceptions;

/**
 *
 * @author gdm
 */
public class DownloaderPluginException extends Exception {

    public DownloaderPluginException(String message) {
        super(message);
    }

    public DownloaderPluginException(Throwable cause) {
        super(cause);
    }
    
    public DownloaderPluginException(String message, Throwable cause) {
        super(message, cause);
    }

    
}
