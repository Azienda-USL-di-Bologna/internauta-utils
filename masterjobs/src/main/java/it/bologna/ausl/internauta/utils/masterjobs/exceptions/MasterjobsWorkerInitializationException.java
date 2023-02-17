package it.bologna.ausl.internauta.utils.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsWorkerInitializationException extends MasterjobsWorkerException {

    public MasterjobsWorkerInitializationException(String message) {
        super(message);
    }

    public MasterjobsWorkerInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MasterjobsWorkerInitializationException(Throwable cause) {
        super(cause);
    }
}
