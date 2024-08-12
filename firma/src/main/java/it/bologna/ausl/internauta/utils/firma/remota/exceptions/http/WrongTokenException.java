package it.bologna.ausl.internauta.utils.firma.remota.exceptions.http;

import it.bologna.ausl.internauta.utils.firma.exceptions.FirmaHttpException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author gdm
 */
//@ResponseStatus(HttpStatus.CONFLICT)
public class WrongTokenException extends FirmaHttpException {

    public WrongTokenException(String message) {
        super(message);
    }

    public WrongTokenException(Throwable cause) {
        super(cause);
    }

    public WrongTokenException(String message, Throwable cause) {
        super(message, cause);
    }

}
