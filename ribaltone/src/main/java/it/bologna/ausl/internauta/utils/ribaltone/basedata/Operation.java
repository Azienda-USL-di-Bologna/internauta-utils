package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import jakarta.persistence.EntityManager;

/**
 *
 * @author Top
 * @param <T>
 */
public abstract class Operation<T extends DatiRibaltoneInterface> {

    public static enum Azione {
        INSERT,
        EDIT,
        CHIUSURA,
        CAMBIO_PADRE,
        RINOMINA,
        UNIFICAZIONE;
    }

    private Azione azione;
    private T entitaCoinvolta;
    private EntityManager entityManager;

    public Operation(Azione azione, T entitaCoinvolta, EntityManager entityManager) {
        this.azione = azione;
        this.entitaCoinvolta = entitaCoinvolta;
        this.entityManager = entityManager;
    }

    public TipologiaCsv getTipo() {
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

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public abstract void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException;
}
