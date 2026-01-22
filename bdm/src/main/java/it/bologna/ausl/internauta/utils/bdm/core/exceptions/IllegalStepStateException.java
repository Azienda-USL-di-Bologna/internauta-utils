package it.bologna.ausl.internauta.utils.bdm.core.exceptions;

/**
 *
 * @author gdm
 */
public class IllegalStepStateException extends BdmExeption {

    public IllegalStepStateException(String message) {
        super(message);
    }

    public IllegalStepStateException(Throwable cause) {
        super(cause);
    }

    public IllegalStepStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
