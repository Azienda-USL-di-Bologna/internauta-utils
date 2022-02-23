package it.bologna.ausl.internauta.utils.downloader.exceptions;

/**
 *
 * @author gdm
 */
public class DownloaderConfigurationException extends Exception {

    public DownloaderConfigurationException(String message) {
        super(message);
    }

    public DownloaderConfigurationException(Throwable cause) {
        super(cause);
    }
    
    public DownloaderConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    
}
