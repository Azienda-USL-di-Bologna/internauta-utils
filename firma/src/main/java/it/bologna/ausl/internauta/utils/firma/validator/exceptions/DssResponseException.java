package it.bologna.ausl.internauta.utils.firma.validator.exceptions;

/**
 *
 * @author gdm
 */
public class DssResponseException extends Exception {

    public DssResponseException(String message) {
        super(message);
    }

    public DssResponseException(Throwable cause) {
        super(cause);
    }

    public DssResponseException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
