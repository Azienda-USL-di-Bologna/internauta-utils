package it.bologna.ausl.middelmine.exceptions;

/**
 *
 * @author Salo
 */
public class RedmineCallerException extends MiddleMineException {

    public RedmineCallerException(String message) {
        super(message);
    }

    public RedmineCallerException(Exception exception) {
        super(exception);
    }

}
