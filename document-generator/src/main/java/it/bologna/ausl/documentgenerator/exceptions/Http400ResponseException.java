package it.bologna.ausl.documentgenerator.exceptions;

import org.springframework.http.HttpStatus;

/**
 *
 * @author spritz
 */
public class Http400ResponseException extends HttpInternautaResponseException {

    public Http400ResponseException(String code, String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, code, message, cause);
    }

    public Http400ResponseException(String code, String message) {
        super(HttpStatus.BAD_REQUEST, code, message);
    }

    public Http400ResponseException(String code, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, code, cause);
    }
}
