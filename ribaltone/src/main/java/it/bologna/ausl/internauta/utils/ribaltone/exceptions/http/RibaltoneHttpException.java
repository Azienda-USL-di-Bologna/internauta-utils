package it.bologna.ausl.internauta.utils.ribaltone.exceptions.http;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 *
 * @author The great Guido
 */
public class RibaltoneHttpException extends RuntimeException {

    public RibaltoneHttpException(String message) {
        super(message);
    }

    public RibaltoneHttpException(Throwable cause) {
        super(cause);
    }

    public RibaltoneHttpException(String message, Throwable cause) {
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
