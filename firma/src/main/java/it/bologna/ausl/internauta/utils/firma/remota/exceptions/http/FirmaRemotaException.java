package it.bologna.ausl.internauta.utils.firma.remota.exceptions.http;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 *
 * @author The great Guido
 */
public class FirmaRemotaException extends Exception {

    public FirmaRemotaException(String message) {
        super(message);
    }

    public FirmaRemotaException(Throwable cause) {
        super(cause);
    }

    public FirmaRemotaException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public Map<String, Object> toMap(HttpStatus status) {
         Map<String, Object> res = new HashMap();
         res.put("message", this.getMessage());
         res.put("exception", getClass().getSimpleName());
         res.put("status", this.getCause());
         res.put("cause", this.getCause());
         res.put("status", status.value());
         return res;
    }
}