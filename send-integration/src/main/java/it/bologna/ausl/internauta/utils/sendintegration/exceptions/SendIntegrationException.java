package it.bologna.ausl.internauta.utils.sendintegration.exceptions;

/**
 *
 * @author gdm
 */
public class  SendIntegrationException extends Exception {

    public SendIntegrationException(String message) {
        super(message);
    }

    public SendIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SendIntegrationException(Throwable cause) {
        super(cause);
    }
}
