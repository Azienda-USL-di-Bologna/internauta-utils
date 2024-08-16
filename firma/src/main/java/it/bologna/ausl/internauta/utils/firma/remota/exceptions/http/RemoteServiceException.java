package it.bologna.ausl.internauta.utils.firma.remota.exceptions.http;

import it.bologna.ausl.internauta.utils.firma.exceptions.FirmaHttpException;

/**
 *
 * @author gdm
 */
//@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class RemoteServiceException extends FirmaHttpException {

    public RemoteServiceException(String message) {
        super(message);
    }

    public RemoteServiceException(Throwable cause) {
        super(cause);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
