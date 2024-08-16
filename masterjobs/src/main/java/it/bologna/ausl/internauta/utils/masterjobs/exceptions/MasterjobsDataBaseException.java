package it.bologna.ausl.internauta.utils.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsDataBaseException extends Exception {

    public MasterjobsDataBaseException(String message) {
        super(message);
    }

    public MasterjobsDataBaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public MasterjobsDataBaseException(Throwable cause) {
        super(cause);
    }
}
