package it.bologna.ausl.internauta.utils.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsRuntimeExceptionWrapper extends RuntimeException {
    
    private final Throwable originalException;

    public MasterjobsRuntimeExceptionWrapper(String string, Throwable originalException) {
        super(string, originalException);
        this.originalException = originalException;
    }

    public MasterjobsRuntimeExceptionWrapper(Throwable originalException) {
        super(originalException);
        this.originalException = originalException;
    }

    public Throwable getOriginalException() {
        return originalException;
    }
}
