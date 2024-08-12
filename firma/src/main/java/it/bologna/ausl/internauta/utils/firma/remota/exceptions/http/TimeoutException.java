package it.bologna.ausl.internauta.utils.firma.remota.exceptions.http;

import it.bologna.ausl.internauta.utils.firma.exceptions.FirmaHttpException;

/**
 *
 * @author gdm
 */
public class TimeoutException extends FirmaHttpException {

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(Throwable cause) {
        super(cause);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
