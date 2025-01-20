package it.bologna.ausl.internauta.utils.ribaltone.basedata;

/**
 *
 * @author Top
 * @param <T>
 */
public abstract class Operation<T extends DatiDaImportareInterface> {
public static enum Azione {
        INSERT,
        EDIT
    }

    Azione azione;
    T entitaCoinvolta;

    public Operation(Azione azione, T entitaCoinvolta) {
        this.azione = azione;
        this.entitaCoinvolta = entitaCoinvolta;
    }

    public abstract void esegui();
}

