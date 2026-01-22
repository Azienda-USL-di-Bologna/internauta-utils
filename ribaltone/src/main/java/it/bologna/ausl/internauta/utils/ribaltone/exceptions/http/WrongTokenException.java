package it.bologna.ausl.internauta.utils.ribaltone.exceptions.http;

/**
 *
 * @author gdm
 */
//@ResponseStatus(HttpStatus.CONFLICT)
public class WrongTokenException extends RibaltoneHttpException {

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
