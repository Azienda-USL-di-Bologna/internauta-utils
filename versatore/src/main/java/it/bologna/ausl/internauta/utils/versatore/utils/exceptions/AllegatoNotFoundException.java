package it.bologna.ausl.internauta.utils.versatore.utils.exceptions;

/**
 *
 * @author Giuseppe Russo <g.russo@nsi.it>
 */
public class AllegatoNotFoundException extends Exception {
    
     public AllegatoNotFoundException(String message) {
        super(message);
    }

    public AllegatoNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public AllegatoNotFoundException(Throwable cause) {
        super(cause);
    }
    
}
