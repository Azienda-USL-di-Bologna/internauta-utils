package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
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
    
    public abstract void dump(Operations operations, String key);
    
    public abstract Operations restore(String key) throws ClassNotFoundException;
    
}
