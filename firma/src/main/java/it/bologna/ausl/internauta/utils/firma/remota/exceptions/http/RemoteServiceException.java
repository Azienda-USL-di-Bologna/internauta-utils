package it.bologna.ausl.internauta.utils.firma.remota.exceptions.http;

/**
 *
 * @author gdm
 */
//@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class RemoteServiceException extends FirmaRemotaHttpException {

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
