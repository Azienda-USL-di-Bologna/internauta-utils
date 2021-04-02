package it.bologna.ausl.documentgenerator.exceptions;

/**
 *
 * @author guido
 */
public class Sql2oSelectException extends Exception {

    public enum SelectException {
        NESSUN_RISULTATO, PIU_RISULTATI
    }

    private SelectException causaEccezione;

    public SelectException getCausaEccezione() {
        return causaEccezione;
    }

    public Sql2oSelectException(SelectException causaEccezione) {
        this.causaEccezione = causaEccezione;
    }

    public Sql2oSelectException(SelectException causaEccezione, String message) {
        super(message);
        this.causaEccezione = causaEccezione;
    }

    public Sql2oSelectException(SelectException causaEccezione, Throwable cause) {
        super(cause);
        this.causaEccezione = causaEccezione;
    }

    public Sql2oSelectException(String message, Throwable cause) {
        super(message, cause);
    }

    public Sql2oSelectException(SelectException causaEccezione, String message, Throwable cause) {
        super(message, cause);
        this.causaEccezione = causaEccezione;
    }

}
