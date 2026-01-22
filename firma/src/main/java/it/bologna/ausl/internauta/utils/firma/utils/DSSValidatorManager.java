package it.bologna.ausl.internauta.utils.firma.utils;

import com.google.common.base.Charsets;
import it.bologna.ausl.dss.data.DSSValidatorReponse;
import it.bologna.ausl.dss.data.exceptions.NoSignException;
import it.bologna.ausl.internauta.utils.firma.configuration.FirmaHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.firma.validator.controllers.ValidatorController;
import it.bologna.ausl.internauta.utils.firma.validator.exceptions.DssResponseException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 *
 * @author gdm
 */
@Component
public class DSSValidatorManager {
    
    private static final Logger log = LoggerFactory.getLogger(ValidatorController.class);
    
    @Autowired
    private ConfigParams configParams;
    
    @Autowired
    private FirmaHttpClientConfiguration firmaHttpClientConfiguration;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public Map<String, String> getReportCertificateValidationMap(MultipartFile file, LocalDateTime validationDate) throws DssResponseException {
        log.info("charset: " + System.getProperty("file.encoding"));
        Map<String, String> res;
        try {
            res = validateCertificate( validationDate, file.getBytes());
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
        } catch (JacksonException ex) {
        }
        return res;
    }

    public DSSValidatorReponse callDssValidator(ConfigParams.ExternalSignAndCertificateValidatorParamsKey paramKey, LocalDateTime validationDate, byte[] file) throws DssResponseException, IOException, NoSignException {
        
        
        String url = configParams.getExternalSignAndCertificateValidator(paramKey);
//        url = "http://localhost:10008/dss-validator-api/validator/validateDocument";
        log.info(String.format("url: %s", url));
        
        OkHttpClient client = firmaHttpClientConfiguration.getHttpClientManager().getOkHttpClient();

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
            .addFormDataPart("file", "file.tmp", okhttp3.RequestBody.create(MediaType.parse("application/octet-stream"), file));
        if (validationDate != null) {
            requestBodyBuilder.addFormDataPart("validationDate", validationDate.atZone(ZoneId.of("Europe/Rome")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")));
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
    
    public List<Map<String, Object>> getSignsReport(LocalDateTime validationDate, byte[] file) throws DssResponseException, IOException, NoSignException {
        DSSValidatorReponse dSSValidatorReponse = callDssValidator(ConfigParams.ExternalSignAndCertificateValidatorParamsKey.validateDocumentUrl,  validationDate, file);
        return dSSValidatorReponse.getSignaturesReport();
    }
    
    public String validateSignedDocument(LocalDateTime validationDate, byte[] file) throws DssResponseException, IOException, NoSignException {
        DSSValidatorReponse dSSValidatorReponse = callDssValidator(ConfigParams.ExternalSignAndCertificateValidatorParamsKey.validateDocumentUrl, validationDate, file);
        return dSSValidatorReponse.getSignReportString();
    }
    
    public Map<String, String> validateCertificate(LocalDateTime validationDate, byte[] file) throws DssResponseException, IOException, NoSignException {
        DSSValidatorReponse dSSValidatorReponse = callDssValidator(ConfigParams.ExternalSignAndCertificateValidatorParamsKey.validateCertificateUrl, validationDate, file);
        return dSSValidatorReponse.getCertificateReportMap();
    }
}
