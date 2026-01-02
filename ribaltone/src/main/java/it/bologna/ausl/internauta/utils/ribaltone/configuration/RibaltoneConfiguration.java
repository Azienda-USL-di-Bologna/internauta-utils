package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import org.springframework.stereotype.Component;

/**
 *
 * @author Top
 */
@Component
public class RibaltoneConfiguration {

    private ObjectMapper objectMapper;

    private MinIOWrapper minIOWrapper;

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MinIOWrapper getMinIOWrapper() {
        return minIOWrapper;
    }

    public void setMinIOWrapper(MinIOWrapper minIOWrapper) {
        this.minIOWrapper = minIOWrapper;
    }

}
