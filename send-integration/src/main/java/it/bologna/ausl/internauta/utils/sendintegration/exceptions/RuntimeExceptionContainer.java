package it.bologna.ausl.internauta.utils.sendintegration.exceptions;

/**
 *
 * @author gdm
 */
public class  RuntimeExceptionContainer extends RuntimeException {

    private Exception exception;

    public RuntimeExceptionContainer(Exception cause) {
        super(cause);
    }

    public Throwable getException() {
        return exception;
    }
}
