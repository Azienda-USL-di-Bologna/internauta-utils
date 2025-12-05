package it.bologna.ausl.internauta.utils.sendintegration.authorization.exceptions;

/**
 *
 * @author gdm
 */
public class NotValidJwtException extends Exception {

    public NotValidJwtException(String message) {
        super(message);
    }

    public NotValidJwtException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotValidJwtException(Throwable cause) {
        super(cause);
    }
    
}
