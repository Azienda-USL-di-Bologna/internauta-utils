package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import it.bologna.ausl.internauta.utils.ribaltone.krint.RibaltoneKrintWrapperManager;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author Top
 */
@Component
public class RibaltoneConfiguration {

    private ObjectMapper objectMapper;

    private MinIOWrapper minIOWrapper;

    private RibaltoneKrintWrapperManager ribaltoneKrintWrapperManager;

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

    public RibaltoneKrintWrapperManager getRibaltoneKrintWrapperManager() {
        return ribaltoneKrintWrapperManager;
    }

    public void setRibaltoneKrintWrapperManager(RibaltoneKrintWrapperManager ribaltoneKrintWrapperManager) {
        this.ribaltoneKrintWrapperManager = ribaltoneKrintWrapperManager;
    }

}
