package it.bologna.ausl.internauta.utils.bdm.core.exceptions;

/**
 *
 * @author gdm
 */
public class BdmRuntimeExceptionContainer extends RuntimeException {

    private Exception exception;

    public BdmRuntimeExceptionContainer(Exception exception) {
        super();
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }
}
