package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author Top
 */
@Component
public class RibaltoneConfiguration {
    
    private ObjectMapper objectMapper;

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }  
}

