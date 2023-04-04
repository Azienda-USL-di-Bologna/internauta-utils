package it.bologna.ausl.internauta.utils.firma.remota.exceptions.http;

/**
 *
 * @author gdm
 */
public class TimeoutException extends FirmaRemotaHttpException {

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
