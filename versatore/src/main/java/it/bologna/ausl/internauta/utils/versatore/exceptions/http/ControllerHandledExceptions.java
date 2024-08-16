package it.bologna.ausl.internauta.utils.versatore.exceptions.http;


import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

public interface ControllerHandledExceptions {

    @ExceptionHandler({WrongTokenException.class, InvalidCredentialException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public default Map<String, Object> handleConflitctException(VersatoreHttpException ex) {
        return ex.toMap(HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler({RemoteFileNotFoundException.class, RemoteServiceException.class})
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public default Map<String, Object> handleBadGatewayException(VersatoreHttpException ex) {
        return ex.toMap(HttpStatus.BAD_GATEWAY);
    }
}