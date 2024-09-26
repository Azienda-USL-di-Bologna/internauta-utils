package it.bologna.ausl.internauta.utils.firma.remota.exceptions.http;


import it.bologna.ausl.internauta.utils.firma.exceptions.FirmaHttpException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.InvalidCredentialException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteFileNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.RemoteServiceException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.WrongTokenException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

public interface FirmaRemotaControllerHandledExceptions {

    @ExceptionHandler({WrongTokenException.class, InvalidCredentialException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public default Map<String, Object> handleConflitctException(FirmaHttpException ex) {
        return ex.toMap(HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler({RemoteFileNotFoundException.class, RemoteServiceException.class})
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public default Map<String, Object> handleBadGatewayException(FirmaHttpException ex) {
        return ex.toMap(HttpStatus.BAD_GATEWAY);
    }
}