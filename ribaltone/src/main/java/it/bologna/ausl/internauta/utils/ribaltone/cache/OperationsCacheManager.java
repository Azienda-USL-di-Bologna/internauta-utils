package it.bologna.ausl.internauta.utils.ribaltone.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneCache;

/**
 *
 * @author Top
 */
public class OperationsCacheManager {
    
    private ObjectMapper objectMapper;
    
    private RibaltoneCache ribaltoneCache;
    
    public OperationsCacheManager(RibaltoneCache ribaltoneCache, ObjectMapper objectMapper) {
        this.objectMapper=objectMapper;
        this.ribaltoneCache = ribaltoneCache;
    }

    public void dump(Operations datiDaImportareValidati) {
        this.ribaltoneCache.dump(datiDaImportareValidati, "key");
    }
    
    public Operations restore(String key) throws ClassNotFoundException {
        return this.ribaltoneCache.restore(key);
    }
}
