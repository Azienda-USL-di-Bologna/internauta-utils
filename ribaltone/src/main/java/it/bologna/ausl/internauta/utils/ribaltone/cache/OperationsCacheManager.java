package it.bologna.ausl.internauta.utils.ribaltone.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;

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
    
    
    public void dump(Operations datiDaImportareValidati) throws RibaltoneHttpException {
        this.ribaltoneCache.dump(datiDaImportareValidati);
    }
    
    public Operations restore() throws ClassNotFoundException, RibaltoneHttpException, JsonProcessingException {
        return this.ribaltoneCache.restore();
    }
}
