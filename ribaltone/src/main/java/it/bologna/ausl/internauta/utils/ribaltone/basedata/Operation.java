package it.bologna.ausl.internauta.utils.ribaltone.basedata;

/**
 *
 * @author Top
 * @param <T>
 */
public abstract class Operation<T extends DatiRibaltoneInterface> {
public static enum Azione {
        INSERT,
        EDIT,
        CHIUSURA
    }

    private Azione azione;
    private T entitaCoinvolta;    

    public Operation(Azione azione, T entitaCoinvolta) {
        this.azione = azione;
        this.entitaCoinvolta = entitaCoinvolta;
    }
    
    public String getTipo(){
        return this.entitaCoinvolta.getTipo();
    }

    public Azione getAzione() {
        return azione;
    }

    public void setAzione(Azione azione) {
        this.azione = azione;
    }

    public T getEntitaCoinvolta() {
        return entitaCoinvolta;
    }

    public void setEntitaCoinvolta(T entitaCoinvolta) {
        this.entitaCoinvolta = entitaCoinvolta;
    }
    
    public abstract void esegui();
}

