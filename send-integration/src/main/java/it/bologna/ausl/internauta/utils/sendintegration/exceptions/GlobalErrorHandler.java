package it.bologna.ausl.internauta.utils.sendintegration.exceptions;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {
//        LottoBaseConEventualiErrori errorResponse = new LottoBaseConEventualiErrori();
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
//        Errore errore = new Errore(null, code);

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
//        errore.setDetail(dettagli.toString());
//        errorResponse.setNumeroDocumenti(1);
//        errorResponse.setErrori(Arrays.asList(errore));
        Map<String, String> resp = new HashMap<>();
        resp.put("descrizione", "Richiesta malformata");
        resp.put("code", code);
        resp.put("detail", dettagli.toString());
        return ResponseEntity.badRequest().body(resp);
    }

    /**
     * Gestisce gli errori manualmente lanciati con ResponseStatusException
     * @param ex
     * @return 
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, String> resp = new HashMap<>();
        resp.put("descrizione", ex.getReason());
        if (ex instanceof SendResponseStatusException sendEx) {
            if (StringUtils.hasText(sendEx.getCode())) {
                resp.put("code", sendEx.getCode());
            }
            if (StringUtils.hasText(sendEx.getDetail())) {
                resp.put("detail", sendEx.getDetail());
            }
        }
        return ResponseEntity.status(ex.getStatusCode()).body(resp);
    }
}