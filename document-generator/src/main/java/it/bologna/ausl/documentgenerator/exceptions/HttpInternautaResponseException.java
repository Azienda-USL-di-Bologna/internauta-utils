package it.bologna.ausl.documentgenerator.exceptions;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 *
 * @author spritz
 */
public abstract class HttpInternautaResponseException extends Exception {

    protected HttpStatus httpStatus;
    protected String code;

    public Map<String, Object> getResponseBody() {
        Map<String, Object> res = new HashMap<>();
        res.put("httpStatus", httpStatus);
        res.put("message", getMessage());
        res.put("code", code);

        return res;
    }

    public HttpInternautaResponseException(HttpStatus httpStatus, String code, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public HttpInternautaResponseException(HttpStatus httpStatus, String code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public HttpInternautaResponseException(HttpStatus httpStatus, String code, Throwable cause) {
        super(cause);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
