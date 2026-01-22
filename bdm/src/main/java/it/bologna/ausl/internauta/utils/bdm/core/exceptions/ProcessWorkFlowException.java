package it.bologna.ausl.internauta.utils.bdm.core.exceptions;

/**
 *
 * @author gdm
 */
public class ProcessWorkFlowException extends BdmExeption {

    public ProcessWorkFlowException(String message) {
        super(message);
    }

    public ProcessWorkFlowException(Throwable cause) {
        super(cause);
    }

    public ProcessWorkFlowException(String message, Throwable cause) {
        super(message, cause);
    }
}
