package it.bologna.ausl.internauta.utils.firma.utils.exceptions;

/**
 *
 * @author gdm
 */
public class EncryptionException extends Exception {

    public EncryptionException(String message) {
        super(message);
    }

    public EncryptionException(Throwable cause) {
        super(cause);
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
