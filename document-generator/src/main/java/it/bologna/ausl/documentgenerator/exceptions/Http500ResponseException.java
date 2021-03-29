package it.bologna.ausl.documentgenerator.exceptions;

import org.springframework.http.HttpStatus;

/**
 *
 * @author spritz
 */
public class Http500ResponseException extends HttpInternautaResponseException {

    public Http500ResponseException(String code, String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, code, message, cause);
    }

    public Http500ResponseException(String code, String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, code, message);
    }

    public Http500ResponseException(String code, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, code, cause);
    }
}
