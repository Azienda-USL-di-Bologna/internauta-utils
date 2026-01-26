package it.bologna.ausl.internauta.utils.firma.validator.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.bologna.ausl.dss.data.exceptions.NoSignException;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams;
import it.bologna.ausl.internauta.utils.firma.utils.ConfigParams;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.firma.utils.DSSValidatorManager;
import it.bologna.ausl.internauta.utils.firma.validator.exceptions.DssResponseException;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;

/**
 * Controller che implementa le API per la validazione dei file firmati
 * 
 * @author gdm
 */
@RestController
@RequestMapping(value = "${firma.validator.mapping.url}")
public class ValidatorController implements FirmaRemotaControllerHandledExceptions {

    private static final Logger log = LoggerFactory.getLogger(ValidatorController.class);
    
    @Autowired
    private ConfigParams configParams;
    
    @Autowired
    private DSSValidatorManager dSSValidatorManager;

    @RequestMapping(value = "/test/{nome}/{cognome}", method = RequestMethod.GET)
    public String test(@PathVariable String nome, @PathVariable String cognome) {

//        System.out.println("ESITO: " + var);
        return "ciao " + nome + cognome;
    }
    
    @RequestMapping(value = "/validateSignedFileFromRepo", method = RequestMethod.GET, produces = "text/plain")
    public ResponseEntity<String> validateSignedFileFromRepo( 
            @RequestParam(value = "fileRepoFileId", required = false) String fileRepoFileId, 
            @RequestParam(value = "fileRepoMongoUuid", required = false) String fileRepoMongoUuid, 
            //@RequestParam(value = "validationDate", required = false) String validationDate,
            @RequestParam(value = "validationDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validationDate,
            HttpServletRequest request) {
        log.info("charset: " + System.getProperty("file.encoding"));
        String res;
        if (!StringUtils.hasText(fileRepoFileId) && !StringUtils.hasText(fileRepoMongoUuid)) {
            String error = "è necessario almeno uno tra fileRepoFileId e fileRepoMongoUuid";
            log.error(error);
            return ResponseEntity.badRequest().body(error);
        } else if (StringUtils.hasText(fileRepoFileId) && StringUtils.hasText(fileRepoMongoUuid)) {
            String message = "Sono stati passati sia fileRepoFileId, che fileRepoMongoUuid, verrà usato fileRepoFileId";
            log.warn(message);
        }
        
        MinIOWrapper minIOWrapper = configParams.getMinIOWrapper();
        try (InputStream fileIs = StringUtils.hasText(fileRepoFileId)? minIOWrapper.getByFileId(fileRepoFileId): minIOWrapper.getByUuid(fileRepoMongoUuid)) {
            if (fileIs != null) {
                byte[] file = IOUtils.toByteArray(fileIs);
                res = dSSValidatorManager.validateSignedDocument( validationDate, file);
            } else {
                String error = "il file non è stato trovato nel repository";
                log.error(error);
                return ResponseEntity.internalServerError().body(error);
            }
        } catch (DssResponseException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        } catch (IOException ex) {
            String error = "errore nella chiamata al validatore DSS";
            log.error(error, ex);
            return ResponseEntity.internalServerError().body(error);
        } catch (MinIOWrapperException ex) {
            String error = "errore nel reperimento del file dal repository";
            log.error(error, ex);
            return ResponseEntity.internalServerError().body(error);
        } catch (NoSignException ex) {
            return ResponseEntity.noContent().build();
        }
//        log.info(res);
        return ResponseEntity.ok(res);
//          return dSSValidatorManager.validateSingleFile(fileRepoFileId, fileRepoMongoUuid, validationDate, request);
    }
    
    @RequestMapping(value = "/validateSignedFile", consumes = "multipart/form-data", method = RequestMethod.POST, produces = "text/plain")
    public ResponseEntity<String> validateSignedFile( 
            @RequestParam("file") MultipartFile file, 
            //@RequestParam(value = "validationDate", required = false) String validationDate,
            @RequestParam(value = "validationDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validationDate,
            HttpServletRequest request) {
        log.info("charset: " + System.getProperty("file.encoding"));
        String res;
        try {
            res = dSSValidatorManager.validateSignedDocument( validationDate, file.getBytes());
        } catch (DssResponseException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        } catch (IOException ex) {
            String error = "errore nella chiamata al validatore DSS";
            log.error(error, ex);
            return ResponseEntity.internalServerError().body(error);
        } catch (NoSignException ex) {
            return ResponseEntity.noContent().build();
        }
//        log.info(res);
        return ResponseEntity.ok(res);
    }
    
    @RequestMapping(value = "/validateCertificateForFirmaJnj", consumes = "multipart/form-data", method = RequestMethod.POST, produces = "text/plain")
    public ResponseEntity<String> validateCertificateForFirmaJnj( 
            @RequestParam("file") MultipartFile file, 
            //@RequestParam(value = "validationDate", required = false) String validationDate,
            @RequestParam(value = "validationDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validationDate,
            HttpServletRequest request) throws JsonProcessingException, DssResponseException {
            
            SignParams.CertificateStatus res = SignParams.CertificateStatus.UNKNOWN;
            Map<String, String> reportCertificateValidationMap = dSSValidatorManager.getReportCertificateValidationMap(file, validationDate);
            if (reportCertificateValidationMap != null && ! reportCertificateValidationMap.isEmpty()) {
                String indication = reportCertificateValidationMap.get("Indication");
                String subIndication = reportCertificateValidationMap.get("SubIndication");
                if (StringUtils.hasText(indication)) {
                    if (indication.toLowerCase().contains("PASSED".toLowerCase())) {
                        res = SignParams.CertificateStatus.GOOD;
                    } else if (indication.toLowerCase().contains("FAILED".toLowerCase())) {
                        res = SignParams.CertificateStatus.REVOKED;
                        if (StringUtils.hasText(subIndication)) {
                            if (subIndication.toLowerCase().contains("EXPIRED".toLowerCase())) {
                                res = SignParams.CertificateStatus.EXPIRED;
                            } else if (subIndication.toLowerCase().contains("NOT_YET_VALID".toLowerCase())) {
                                res = SignParams.CertificateStatus.NOT_YET_VALID;
                            }
                        }
                    } else if (indication.toLowerCase().contains("INDETERMINATE".toLowerCase())) {
                        res = SignParams.CertificateStatus.UNKNOWN;
                        if (StringUtils.hasText(subIndication)) {
                            if (subIndication.toLowerCase().contains("REVOKED".toLowerCase()) && 
                                    !subIndication.toLowerCase().contains("NOT_REVOKED".toLowerCase())) {
                                res = SignParams.CertificateStatus.REVOKED;
                            } else if (subIndication.toLowerCase().contains("EXPIRED".toLowerCase())) {
                                res = SignParams.CertificateStatus.EXPIRED;
                            } else if (subIndication.toLowerCase().contains("NOT_YET_VALID".toLowerCase())) {
                                res = SignParams.CertificateStatus.NOT_YET_VALID;
                            } else if (subIndication.toLowerCase().contains("OUT_OF_BOUND".toLowerCase())) {
                                res = SignParams.CertificateStatus.EXPIRED;
                            }
                        }
                    }
                }
            }
        return ResponseEntity.ok(res.toString());
    }
    
    @RequestMapping(value = "/validateCertificate", consumes = "multipart/form-data", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<?> validateCertificate( 
            @RequestParam("file") MultipartFile file, 
            @RequestParam(value = "validationDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validationDate,
            HttpServletRequest request) throws JsonProcessingException {
        
        try {
            Map<String, String> reportCertificateValidationMap = dSSValidatorManager.getReportCertificateValidationMap(file, validationDate);
            return ResponseEntity.ok(reportCertificateValidationMap);
        } catch (DssResponseException ex) {
            if (ex.getResponseMap() != null) {
                return ResponseEntity.internalServerError().body(ex.getResponseMap());
            } else {
                ResponseEntity.internalServerError().body(ex.getMessage());
            }
        } catch (Throwable ex) {
            String errorMessage = "errore nella verifica del certificato";
            log.error(errorMessage, ex);
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
        return null;
    }
    
    @RequestMapping(value = "/getSignsReportFromRepo", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getSignsReportFromRepo( 
            @RequestParam(value = "fileRepoFileId", required = false) String fileRepoFileId, 
            @RequestParam(value = "fileRepoMongoUuid", required = false) String fileRepoMongoUuid, 
            //@RequestParam(value = "validationDate", required = false) String validationDate,
            @RequestParam(value = "validationDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validationDate,
            HttpServletRequest request) {
        log.info("charset: " + System.getProperty("file.encoding"));
        List<Map<String, Object>> signsReport;
        if (!StringUtils.hasText(fileRepoFileId) && !StringUtils.hasText(fileRepoMongoUuid)) {
            String error = "è necessario almeno uno tra fileRepoFileId e fileRepoMongoUuid";
            log.error(error);
            return ResponseEntity.badRequest().body(error);
        } else if (StringUtils.hasText(fileRepoFileId) && StringUtils.hasText(fileRepoMongoUuid)) {
            String message = "Sono stati passati sia fileRepoFileId, che fileRepoMongoUuid, verrà usato fileRepoFileId";
            log.warn(message);
        }
        
        MinIOWrapper minIOWrapper = configParams.getMinIOWrapper();
        try (InputStream fileIs = StringUtils.hasText(fileRepoFileId)? minIOWrapper.getByFileId(fileRepoFileId): minIOWrapper.getByUuid(fileRepoMongoUuid)) {
            if (fileIs != null) {
                byte[] file = IOUtils.toByteArray(fileIs);
                signsReport = dSSValidatorManager.getSignsReport( validationDate, file);
                if (signsReport == null) {
                    throw new NoSignException("eccezione lanciata nel caso il validatore non ha riconosciuto il formato del file, probabilmente non è fiomato");
                }
            } else {
                String error = "il file non è stato trovato nel repository";
                log.error(error);
                return ResponseEntity.internalServerError().body(error);
            }
        } catch (DssResponseException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        } catch (IOException ex) {
            String error = "errore nella chiamata al validatore DSS";
            log.error(error, ex);
            return ResponseEntity.internalServerError().body(error);
        } catch (MinIOWrapperException ex) {
            String error = "errore nel reperimento del file dal repository";
            log.error(error, ex);
            return ResponseEntity.internalServerError().body(error);
        } catch (NoSignException ex) {
            return ResponseEntity.noContent().build();
        }
        //slog.info(signsReport.toString());
        return ResponseEntity.ok(signsReport);
    }
    

    
    
    
}