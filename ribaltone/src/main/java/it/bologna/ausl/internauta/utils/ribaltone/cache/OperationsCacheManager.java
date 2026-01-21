package it.bologna.ausl.internauta.utils.ribaltone.cache;

import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author Top
 */
public class OperationsCacheManager {

    private static final Logger log = LoggerFactory.getLogger(OperationsCacheManager.class);

    private ObjectMapper objectMapper;
    private RibaltoneCache ribaltoneCache;

    public OperationsCacheManager(RibaltoneCache ribaltoneCache, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.ribaltoneCache = ribaltoneCache;
    }

    public void dump(Operations datiDaImportareValidati) throws RibaltoneHttpException {
        this.ribaltoneCache.dump(datiDaImportareValidati);
    }

    public Operations restore() throws ClassNotFoundException, RibaltoneHttpException, JacksonException {
        return this.ribaltoneCache.restore();
    }
}
