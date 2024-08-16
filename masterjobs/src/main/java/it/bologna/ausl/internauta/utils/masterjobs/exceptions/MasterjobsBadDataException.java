package it.bologna.ausl.internauta.utils.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsBadDataException extends Exception {

    public MasterjobsBadDataException(String message) {
        super(message);
    }

    public MasterjobsBadDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public MasterjobsBadDataException(Throwable cause) {
        super(cause);
    }
}
