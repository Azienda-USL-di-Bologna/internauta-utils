package it.bologna.ausl.internauta.utils.versatore.exceptions;

import it.bologna.ausl.model.entities.versatore.Versamento;

/**
 *
 * @author gdm
 */
public class VersatoreServiceException extends Exception {
    private Versamento.StatoVersamento statoVersamento;

    public VersatoreServiceException(Versamento.StatoVersamento statoVersamento, String message) {
        super(message);
        this.statoVersamento = statoVersamento;
    }
    
    public VersatoreServiceException(String message) {
        super(message);
    }

    public VersatoreServiceException(Throwable cause) {
        super(cause);
    }

    public VersatoreServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public Versamento.StatoVersamento getStatoVersamento() {
        return statoVersamento;
    }
}
