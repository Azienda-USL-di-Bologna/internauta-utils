package it.bologna.ausl.internauta.utils.firma.remota.exceptions.http;

import it.bologna.ausl.internauta.utils.firma.exceptions.FirmaHttpException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author gdm
 */
//@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidCredentialException extends FirmaHttpException {

    public InvalidCredentialException(String message) {
        super(message);
    }

    public InvalidCredentialException(Throwable cause) {
        super(cause);
    }

    public InvalidCredentialException(String message, Throwable cause) {
        super(message, cause);
    }

}
