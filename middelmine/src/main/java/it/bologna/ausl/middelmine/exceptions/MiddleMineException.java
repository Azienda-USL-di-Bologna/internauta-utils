package it.bologna.ausl.middelmine.exceptions;

/**
 *
 * @author Salo
 */
public abstract class MiddleMineException extends Exception {

    public MiddleMineException(String message) {
        super(message);
    }

    public MiddleMineException(Exception exception) {
        super(exception);
    }

    public MiddleMineException(String message, Exception exception) {
        super(message, exception);
    }
}
