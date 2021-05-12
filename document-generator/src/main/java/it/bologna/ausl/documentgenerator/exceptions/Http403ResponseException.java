package it.bologna.ausl.documentgenerator.exceptions;

import org.springframework.http.HttpStatus;

/**
 *
 * @author spritz
 */
public class Http403ResponseException extends HttpInternautaResponseException {

    public Http403ResponseException(String code, String message, Throwable cause) {
        super(HttpStatus.FORBIDDEN, code, message, cause);
    }

    public Http403ResponseException(String code, String message) {
        super(HttpStatus.FORBIDDEN, code, message);
    }

    public Http403ResponseException(String code, Throwable cause) {
        super(HttpStatus.FORBIDDEN, code, cause);
    }
}
