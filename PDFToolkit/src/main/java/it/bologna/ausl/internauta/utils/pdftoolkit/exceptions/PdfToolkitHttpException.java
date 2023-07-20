package it.bologna.ausl.internauta.utils.pdftoolkit.exceptions;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 *
 * @author Giuseppe Russo <g.russo@dilaxia.com>
 */
public class PdfToolkitHttpException extends Exception {
    
     public PdfToolkitHttpException(String message) {
        super(message);
    }

    public PdfToolkitHttpException(Throwable cause) {
        super(cause);
    }

    public PdfToolkitHttpException(String message, Throwable cause) {
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
