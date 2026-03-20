package it.bologna.ausl.internauta.utils.ribaltone.cache;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.model.entities.baborg.Utente;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author Top
 */
public abstract class RibaltoneCache {

    private static final Logger log = LoggerFactory.getLogger(RibaltoneCache.class);

    public static RibaltoneCache build(Map<String, Object> cacheConfig, ObjectMapper objectMapper, EntityManager entityManager) {
        switch (cacheConfig.get("tipo").toString()) {
            case "REDIS":
                return new RibaltoneCacheRedis(objectMapper, cacheConfig, entityManager);
            default:
                return null;
        }
    }

    public abstract void dump(Operations operations) throws RibaltoneHttpException;

    public abstract Operations restore() throws ClassNotFoundException, RibaltoneHttpException, JacksonException;

    public abstract Boolean isExecuting();

    public abstract void setExecuting(Boolean executing, Utente user);

    public abstract Integer getIdUserExecuting() throws RibaltoneHttpException, JacksonException;

    public abstract Boolean isImportingCSV();

    public abstract void setImportingCSV(Boolean executing, Utente user);

    public abstract Integer getIdUserImportingCSV() throws RibaltoneHttpException, JacksonException;

    public abstract void cleanDataCache();
}
