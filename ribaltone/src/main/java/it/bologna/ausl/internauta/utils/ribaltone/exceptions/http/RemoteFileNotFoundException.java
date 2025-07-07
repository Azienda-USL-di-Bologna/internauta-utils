package it.bologna.ausl.internauta.utils.ribaltone.exceptions.http;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author gdm
 */
//@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class RemoteFileNotFoundException extends RibaltoneHttpException {

    public RemoteFileNotFoundException(String message) {
        super(message);
    }

    public RemoteFileNotFoundException(Throwable cause) {
        super(cause);
    }

    public RemoteFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

}
