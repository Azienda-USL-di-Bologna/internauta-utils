package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CsvImportManager;
import jakarta.persistence.EntityManager;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public abstract Operations restore() throws ClassNotFoundException, RibaltoneHttpException, JsonProcessingException;

    public abstract void cleanCache();
}
