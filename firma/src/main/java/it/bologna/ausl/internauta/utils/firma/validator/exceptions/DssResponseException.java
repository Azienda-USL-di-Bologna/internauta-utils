package it.bologna.ausl.internauta.utils.firma.validator.exceptions;

import java.util.Map;

/**
 *
 * @author gdm
 */
public class DssResponseException extends Exception {

    private Map<String, String> responseMap;
    
    public DssResponseException(String message) {
        super(message);
    }

    public DssResponseException(Throwable cause) {
        super(cause);
    }

    public DssResponseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DssResponseException(String message, Map<String, String> responseMap, Throwable cause) {
        super(message, cause);
        this.responseMap = responseMap;
    }
    
    public DssResponseException(String message, Map<String, String> responseMap) {
        super(message);
        this.responseMap = responseMap;
    }

    public Map<String, String> getResponseMap() {
        return responseMap;
    }
}
