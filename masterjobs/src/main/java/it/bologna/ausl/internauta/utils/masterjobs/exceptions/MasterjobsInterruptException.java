package it.bologna.ausl.internauta.utils.masterjobs.exceptions;

/**
 *
 * @author gdm
 */
public class MasterjobsInterruptException extends RuntimeException {

    public static enum InterruptType{
        PAUSE, STOP
    }
    
    private final InterruptType interruptType;
    
    public MasterjobsInterruptException(InterruptType interruptType) {
        super();
        this.interruptType = interruptType;
    }

    public InterruptType getInterruptType() {
        return interruptType;
    }
}
