package it.bologna.ausl.internauta.utils.ribaltone.basedata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CsvImportManager;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Top
 * @param <T>
 */
public abstract class Operation<T extends DatiRibaltoneInterface> {

    private static final Logger log = LoggerFactory.getLogger(Operation.class);

    public static enum Azione {
        INSERT,
        EDIT,
        CHIUSURA,
        CAMBIO_PADRE,
        RINOMINA,
        UNIFICAZIONE,
        CONFLUENZA;
    }

    private Azione azione;
    private T entitaCoinvolta;
    private Map<String, String> descrizioniAggiuntive;

    @JsonIgnore
    private EntityManager entityManager;

    public Operation(Azione azione, T entitaCoinvolta, EntityManager entityManager, Map<String, String> descrizioniAggiuntive) {
        this.azione = azione;
        this.entitaCoinvolta = entitaCoinvolta;
        this.entityManager = entityManager;
        this.descrizioniAggiuntive = descrizioniAggiuntive;

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

    public Map<String, String> getDescrizioniAggiuntive() {
        return descrizioniAggiuntive;
    }

    public void setDescrizioniAggiuntive(Map<String, String> descrizioniAggiuntive) {
        this.descrizioniAggiuntive = descrizioniAggiuntive;
    }

    public abstract void esegui(Object workToDo, RepositoryFactory repositoryFactory) throws RibaltoneHttpException;
}
