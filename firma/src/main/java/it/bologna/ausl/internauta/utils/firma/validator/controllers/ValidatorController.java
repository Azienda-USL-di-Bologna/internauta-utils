package it.bologna.ausl.internauta.utils.firma.validator.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import it.bologna.ausl.dss.data.DSSValidatorReponse;
import it.bologna.ausl.dss.data.exceptions.NoSignException;
import it.bologna.ausl.internauta.utils.firma.configuration.FirmaHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams;
import it.bologna.ausl.internauta.utils.firma.utils.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.repositories.RequestParameterRepository;
import it.bologna.ausl.internauta.utils.firma.utils.CommonUtils;
import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
import it.bologna.ausl.internauta.utils.firma.utils.ConfigParams.ExternalSignAndCertificateValidatorParamsKey;
import it.bologna.ausl.internauta.utils.firma.validator.exceptions.DssResponseException;
import it.bologna.ausl.minio.manager.MinIOWrapper;
import it.bologna.ausl.minio.manager.exceptions.MinIOWrapperException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.springframework.format.annotation.DateTimeFormat;

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
    private RequestParameterRepository requestParameterRepository;
    
    @Autowired
    private FirmaHttpClientConfiguration firmaHttpClientConfiguration;
    
    @Autowired
    private ObjectMapper objectMapper;

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
                res = validateSignedDocument(request, validationDate, file);
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
        log.info(res);
        return ResponseEntity.ok(res);
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
            res = validateSignedDocument(request, validationDate, file.getBytes());
        } catch (DssResponseException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        } catch (IOException ex) {
            String error = "errore nella chiamata al validatore DSS";
            log.error(error, ex);
            return ResponseEntity.internalServerError().body(error);
        } catch (NoSignException ex) {
            return ResponseEntity.noContent().build();
        }
        log.info(res);
        return ResponseEntity.ok(res);
    }
    
    @RequestMapping(value = "/validateCertificateForFirmaJnj", consumes = "multipart/form-data", method = RequestMethod.POST, produces = "text/plain")
    public ResponseEntity<String> validateCertificateForFirmaJnj( 
            @RequestParam("file") MultipartFile file, 
            //@RequestParam(value = "validationDate", required = false) String validationDate,
            @RequestParam(value = "validationDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime validationDate,
            HttpServletRequest request) throws JsonProcessingException, DssResponseException {
            
            SignParams.CertificateStatus res = SignParams.CertificateStatus.UNKNOWN;
            Map<String, String> reportCertificateValidationMap = getReportCertificateValidationMap(file, validationDate, request);
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
            Map<String, String> reportCertificateValidationMap = getReportCertificateValidationMap(file, validationDate, request);
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
    
    private Map<String, String> getReportCertificateValidationMap(MultipartFile file, LocalDateTime validationDate, HttpServletRequest request) throws DssResponseException {
        log.info("charset: " + System.getProperty("file.encoding"));
        Map<String, String> res;
        try {
            res = validateCertificate(request, validationDate, file.getBytes());
        } catch (DssResponseException ex) {
            res = new HashMap<>();
            res.put("Errors", ex.getMessage());
            throw new DssResponseException("errore nella validazione del certificato", res, ex);
        } catch (IOException ex) {
            String error = "errore nella chiamata al validatore DSS";
            log.error(error, ex);
            res = new HashMap<>();
            res.put("Errors", error + ": " + ex.getMessage());
            throw new DssResponseException("errore nella validazione del certificato", res, ex);
//            return ResponseEntity.internalServerError().body(res);
        } catch (NoSignException ex) {
            String error = "errore nella chiamata al validatore DSS, questo errore non dovrebbe mai accadere";
            log.error(error, ex);
            res = new HashMap<>();
            res.put("Errors", error + ": " + ex.getMessage());
            throw new DssResponseException("errore nella validazione del certificato", res, ex);
        }
        try {
            log.info(this.objectMapper.writeValueAsString(res));
        } catch (JsonProcessingException ex) {
        }
        return res;
    }

    private DSSValidatorReponse callDssValidator(ExternalSignAndCertificateValidatorParamsKey paramKey, HttpServletRequest request, LocalDateTime validationDate, byte[] file) throws DssResponseException, IOException, NoSignException {
        String scheme = request.getScheme();
        String hostname = CommonUtils.getHostname(request);
        Integer port = request.getServerPort();
        
        log.info(String.format("scheme: %s, hostname: %s, port: %s", scheme, hostname, port));
        
        String url = configParams.getExternalSignAndCertificateValidator(paramKey, scheme, hostname, port);
        
        log.info(String.format("url: %s", url));
        
        OkHttpClient client = firmaHttpClientConfiguration.getHttpClientManager().getOkHttpClient();

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
            .addFormDataPart("file", "file.tmp", okhttp3.RequestBody.create(MediaType.parse("application/octet-stream"), file));
        if (validationDate != null) {
            requestBodyBuilder.addFormDataPart("validationDate", validationDate.atZone(ZoneId.of("Europe/Rome")).format(DateTimeFormatter.ISO_DATE_TIME));
        }
        okhttp3.RequestBody requestBody = requestBodyBuilder.build();
        Response resp = client.newCall(
                new Request.Builder()
                    .url(url)
                    .post(requestBody).build()).execute();

        if (resp.isSuccessful() && resp.body() != null) {
            byte[] respBytes = resp.body().bytes();
            if (respBytes != null) {
                String resString = new String(respBytes, Charsets.UTF_8);
    //            String resString = new String(resp.body().bytes(), Charsets.ISO_8859_1);
                if (StringUtils.hasText(resString)) {
                    log.info(resString);
                    DSSValidatorReponse dSSValidatorReponse = DSSValidatorReponse.parseFromJson(resString);
                    return dSSValidatorReponse;
                } else {
                    String error = "la chiamata al validatore DSS ha tornato una risposta vuota";
                    log.error(error);
                    throw new DssResponseException(error);
                }
            } else {
                String error = "la chiamata al validatore DSS ha tornato una risposta vuota";
                log.error(error);
                throw new DssResponseException(error);
            }
        } else {
            String errorBody = null;
            if (resp.body() != null) {
                errorBody = resp.body().string();
            }
            String error = String.format("la chiamata al validatore DSS ha tornato errore, codice: %s, errore: %s", resp.code(), errorBody != null ? errorBody: "null");
            log.error(error);
            throw new DssResponseException(error);
        }
    }
    
    private String validateSignedDocument(HttpServletRequest request, LocalDateTime validationDate, byte[] file) throws DssResponseException, IOException, NoSignException {
        DSSValidatorReponse dSSValidatorReponse = callDssValidator(ExternalSignAndCertificateValidatorParamsKey.validateDocumentUrl, request, validationDate, file);
        return dSSValidatorReponse.getSignReportString();
    }
    
    private Map<String, String> validateCertificate(HttpServletRequest request, LocalDateTime validationDate, byte[] file) throws DssResponseException, IOException, NoSignException {
        DSSValidatorReponse dSSValidatorReponse = callDssValidator(ExternalSignAndCertificateValidatorParamsKey.validateCertificateUrl, request, validationDate, file);
        return dSSValidatorReponse.getCertificateReportMap();
    }
}