package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

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

