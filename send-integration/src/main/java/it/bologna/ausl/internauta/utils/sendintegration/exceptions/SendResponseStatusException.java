package it.bologna.ausl.internauta.utils.sendintegration.exceptions;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author gdm
 */
public class  SendResponseStatusException extends ResponseStatusException {

    private String code;
    private String detail;

    public SendResponseStatusException(HttpStatusCode status, String reason) {
        super(status, reason);
    }

    public SendResponseStatusException(HttpStatusCode status, String reason, Throwable cause) {
        super(status, reason, cause);
    }
    
    public SendResponseStatusException(HttpStatusCode status, String reason, String code, String detail) {
        super(status, reason);
        this.code = code;
        this.detail = detail;
    }

    public SendResponseStatusException(HttpStatusCode status, String reason, String code, String detail, Throwable cause) {
        super(status, reason, cause);
        this.code = code;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }
}
