package it.bologna.ausl.internauta.utils.downloader.exceptions;

/**
 *
 * @author gdm
 */
public class DownloaderUploadException extends DownloaderPluginException {

    public DownloaderUploadException(String message) {
        super(message);
    }

    public DownloaderUploadException(Throwable cause) {
        super(cause);
    }
    
    public DownloaderUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    
}
