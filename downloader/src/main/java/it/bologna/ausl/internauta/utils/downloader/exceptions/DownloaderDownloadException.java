package it.bologna.ausl.internauta.utils.downloader.exceptions;

/**
 *
 * @author gdm
 */
public class DownloaderDownloadException extends Exception {

    public DownloaderDownloadException(String message) {
        super(message);
    }

    public DownloaderDownloadException(Throwable cause) {
        super(cause);
    }
    
    public DownloaderDownloadException(String message, Throwable cause) {
        super(message, cause);
    }

    
}
