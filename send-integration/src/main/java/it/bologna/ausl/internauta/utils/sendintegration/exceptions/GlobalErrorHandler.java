package it.bologna.ausl.internauta.utils.sendintegration.exceptions;


import it.bologna.ausl.internauta.utils.send_integration.model.Errore;
import it.bologna.ausl.internauta.utils.send_integration.model.LottoBaseConEventualiErrori;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalErrorHandler {

    /**
     * Gestisce gli errori di validazione generati da @Valid
     * @param ex
     * @return 
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Errore> handleValidationException(MethodArgumentNotValidException ex) {
        LottoBaseConEventualiErrori errorResponse = new LottoBaseConEventualiErrori();
        String code = null;
        if (ex.getBindingResult().getFieldErrors() != null && !ex.getBindingResult().getFieldErrors().isEmpty()) {
            if (ex.getBindingResult().getFieldErrors().stream().anyMatch(f -> f.getCode() != null && f.getCode().equals("NotNull"))) {
                code = "STRUTTURA_ ERRATA";
            } else {
                if (ex.getBindingResult().getFieldError() != null && ex.getBindingResult().getFieldError().getCode() != null) {
                    switch (ex.getBindingResult().getFieldError().getCode()) {
                        case "NotNull" -> code = "STRUTTURA_ ERRATA";
                        case "Pattern" -> code = "PATTERN_ NON_VALIDO";
                    }
                }
            }
        }
        Errore errore = new Errore(null, code);

        List<String> dettagli = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(err -> {
                    if (err instanceof FieldError field) {
                        return field.getField() + ": " + field.getDefaultMessage();
                    }
                    return err.getDefaultMessage();
                })
                .toList();
        errore.setDetail(dettagli.toString());
        errorResponse.setNumeroDocumenti(1);
        errorResponse.setErrori(Arrays.asList(errore));
        return ResponseEntity.badRequest().body(errore);
    }

    /**
     * Gestisce gli errori manualmente lanciati con ResponseStatusException
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<LottoBaseConEventualiErrori> handleResponseStatusException(
            ResponseStatusException ex
    ) {
        if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
            LottoBaseConEventualiErrori errorResponse = new LottoBaseConEventualiErrori();
            Errore errore = new Errore(null, "RICHIESTA_MALFORMATA");

        errore.setDetail(ex.getMessage());
        errorResponse.setNumeroDocumenti(1);
        errorResponse.setErrori(Arrays.asList(errore));
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Lascia passare altri status
        return ResponseEntity.status(ex.getStatusCode()).build();
    }
}