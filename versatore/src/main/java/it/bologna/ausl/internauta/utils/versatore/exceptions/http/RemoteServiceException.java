package it.bologna.ausl.internauta.utils.versatore.exceptions.http;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author gdm
 */
//@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class RemoteServiceException extends VersatoreHttpException {

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
