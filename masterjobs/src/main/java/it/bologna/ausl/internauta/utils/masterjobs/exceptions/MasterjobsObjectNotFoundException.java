package it.bologna.ausl.internauta.utils.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsObjectNotFoundException extends Exception {

    public MasterjobsObjectNotFoundException(String message) {
        super(message);
    }

    public MasterjobsObjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public MasterjobsObjectNotFoundException(Throwable cause) {
        super(cause);
    }
}
