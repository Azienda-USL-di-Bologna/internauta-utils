package it.bologna.ausl.internauta.utils.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsQueuingException extends Exception {

    public MasterjobsQueuingException(String message) {
        super(message);
    }

    public MasterjobsQueuingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MasterjobsQueuingException(Throwable cause) {
        super(cause);
    }
}
