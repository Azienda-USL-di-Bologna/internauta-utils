package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import java.util.Map;

/**
 *
 * @author Top
 */

public abstract class RibaltoneCache {

    public static RibaltoneCache build(Map<String,Object> cacheConfig, ObjectMapper objectMapper){
        switch (cacheConfig.get("tipo").toString()){
            case "REDIS":
                return new RibaltoneCacheRedis(objectMapper,cacheConfig);
            default:
                return null;
        }
    }
    
    public abstract void dump(Operations operations) throws RibaltoneHttpException;
    
    public abstract Operations restore() throws ClassNotFoundException, RibaltoneHttpException, JsonProcessingException;
    
    public abstract void cleanCache();    
}
