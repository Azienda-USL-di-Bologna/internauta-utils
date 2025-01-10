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
    
    //private EntityManager entityManager;

//    public EntityManager getEntityManager() {
//        return entityManager;
//    }
//
//    public void setEntityManager(EntityManager entityManager) {
//        this.entityManager = entityManager;
//    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }  
}
