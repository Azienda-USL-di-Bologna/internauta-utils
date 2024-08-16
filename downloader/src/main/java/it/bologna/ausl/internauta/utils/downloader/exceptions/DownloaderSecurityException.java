package it.bologna.ausl.internauta.utils.downloader.exceptions;

/**
 *
 * @author gdm
 */
public class DownloaderSecurityException extends Exception {

    public DownloaderSecurityException(String message) {
        super(message);
    }

    public DownloaderSecurityException(Throwable cause) {
        super(cause);
    }
    
    public DownloaderSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    
}
