package it.bologna.ausl.internauta.utils.firma.jnj.exceptions;

/**
 *
 * @author gdm
 */
public class FirmaJnJRequestParameterExpiredException extends FirmaJnJException {

    public FirmaJnJRequestParameterExpiredException(String message) {
        super(message);
    }

    public FirmaJnJRequestParameterExpiredException(Throwable cause) {
        super(cause);
    }

    public FirmaJnJRequestParameterExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
