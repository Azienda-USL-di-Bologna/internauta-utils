package it.bologna.ausl.internauta.utils.firma.jnj.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.authorizationutils.AuthorizationUtilityFunctions;
import it.bologna.ausl.internauta.utils.firma.configuration.FirmaHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.firma.data.exceptions.SignParamsException;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams.CertificateStatus;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJException;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJRequestParameterExpiredException;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJRequestParameterNotFoundException;
import it.bologna.ausl.internauta.utils.firma.utils.ConfigParams;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.firma.repositories.RequestParameterRepository;
import it.bologna.ausl.internauta.utils.firma.utils.CommonUtils;
import it.bologna.ausl.model.entities.firma.RequestParameter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.ByteString;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller che implementa le API per la firma remota
 * 
 * Ogni servlet, come prima cosa, reperisce l'istanza corretta della classe di firma (implementazione di FirmaRemota) basandosi sul provider passato in input.
 * L'istanza viene reperita tramite la classe factory FirmaRemotaFactory
 * 
 * @author gdm
 */
@RestController
@RequestMapping(value = "${firma.jnj.mapping.url}")
public class FirmaJnJRestController implements ControllerHandledExceptions {

    private static final Logger log = LoggerFactory.getLogger(FirmaJnJRestController.class);
    
    @Autowired
    private ConfigParams configParams;
    
    @Autowired
    private RequestParameterRepository requestParameterRepository;
    
    @Autowired
    private FirmaHttpClientConfiguration firmaHttpClientConfiguration;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${firma.jnj.mapping.url}")
    private String firmaJnJMappingUrl;
    
    @Value("${firma.jnj.params-expire-seconds}")
    private Integer firmaJnJParamsExpireSeconds;
    
    @Value("${firma.jnj.client-info-file}")
    private String clientInfoFilePath;

    @RequestMapping(value = "/test/{nome}/{cognome}", method = RequestMethod.GET)
    public String test(@PathVariable String nome, @PathVariable String cognome) {

//        System.out.println("ESITO: " + var);
        return "ciao " + nome + cognome;
    }
    
    /**
     * torna i parametri (settati con la setParameters) per la sessione di firma jnj
     * @param token il token che identifica i parametri, tornato dalla setParameters
     * @param extendedValidity se true, prolunga la validità dei parametri di mezzora (serve per quando l'applicazione jnj-client riparte dopo un aggiornamento)
     * @param request
     * @return
     * @throws FirmaJnJException 
     */
    @RequestMapping(value = "/getParameters", method = RequestMethod.GET)
    public SignParams getParameters(
            @RequestParam(required = true) String token, 
            @RequestParam(required = false, defaultValue = "false") Boolean extendedValidity, 
            HttpServletRequest request) throws FirmaJnJException {
        Optional<RequestParameter> requestParamOptional = requestParameterRepository.findById(token);
        
        SignParams res = null;
        if (requestParamOptional.isPresent()) {
            RequestParameter requestParameter = requestParamOptional.get();
            if (
                    (requestParameter.getExpireOn().isAfter(ZonedDateTime.now())) || 
                    (extendedValidity && requestParameter.getExpireOn().plusMinutes(30).isAfter(ZonedDateTime.now()))
                ) {
                res = objectMapper.convertValue(requestParameter.getData(), SignParams.class);
                res.setServerUrl(getFirmaJnJServerUrl(request));
            } else {
                throw new FirmaJnJRequestParameterExpiredException(String.format("RequestParamter %s scaduto", token));
            }
        } else {
            throw new FirmaJnJRequestParameterNotFoundException(String.format("RequestParamter %s non trovato", token));
        }
        
        return res;
    }

    @RequestMapping(value = "/setParameters", method = RequestMethod.POST)
    public String setParameters(@RequestBody(required = true) SignParams signParams) throws SignParamsException {
        RequestParameter requestParameter = new RequestParameter();
        String token = UUID.randomUUID().toString();
        requestParameter.setId(token);
        requestParameter.setData(signParams.toMap());
        requestParameter.setExpireOn(ZonedDateTime.now().plusSeconds(firmaJnJParamsExpireSeconds));
        RequestParameter savedRequestParamter = requestParameterRepository.save(requestParameter);
        return token;
    }
    
    @RequestMapping(value = "/checkUpdate", method = RequestMethod.GET, produces = "application/octet-stream")
    public void checkUpdate(@RequestParam(required = true) String version, HttpServletResponse response) throws IOException {
        File clientInfoFile = new File(this.clientInfoFilePath);
        if (clientInfoFile.exists()) {
            Map<String, Object> clientInfo = objectMapper.readValue(clientInfoFile, new TypeReference<Map<String, Object>>(){});
            String actualVersion = (String) clientInfo.get("version");
            String msiInstallerFileName = (String) clientInfo.get("msi-file");
            DefaultArtifactVersion versionObject = new DefaultArtifactVersion(version);
            DefaultArtifactVersion actualVersionObject = new DefaultArtifactVersion(actualVersion);
            if (actualVersionObject.compareTo(versionObject) > 0) {
                response.setStatus(HttpStatus.CREATED.value());
                log.info(String.format("new version detected: client: %s server %s", version, actualVersion));
                ServletOutputStream outputStream = response.getOutputStream();
                File msiInstallerFile = new File(msiInstallerFileName);
                Files.copy(msiInstallerFile.toPath(), outputStream);
            } else {
                response.setStatus(HttpStatus.OK.value());
            }
        } else {
            log.warn(String.format("client info file not found in path: %s", clientInfoFile.getAbsolutePath()));
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    @RequestMapping(value = "/checkCertificateStatus", consumes = "multipart/form-data", method = RequestMethod.POST, produces = "text/plain")
    public String checkCertificateStatus( 
            @RequestParam("file") MultipartFile file, 
            HttpServletRequest request) throws IOException {
        CertificateStatus res = CertificateStatus.UNKNOWN;
        try {
            // controllo se il certificato è scaduto o non è ancora valido
            res = getOfflineCertificateStatus(file.getBytes());
            
            
            // se il cerificato è valido, controllo se è stato revocato
            if (res == CertificateStatus.GOOD) {
                res = CertificateStatus.UNKNOWN;
                // per il controllo faccio la chiamata al servizio di controllo esterno (attualmente firmasemplice)
                String scheme = request.getScheme();
                String hostname = CommonUtils.getHostname(request);
                Integer port = request.getServerPort();
                String externalCheckCertificateUrl = configParams.getExternalCheckCertificateUrl(scheme, hostname, port);
                OkHttpClient client = firmaHttpClientConfiguration.getHttpClientManager().getOkHttpClient();

                okhttp3.RequestBody requestBody = new MultipartBody.Builder()
                        .addFormDataPart("file", 
                            "certificate.crt", 
                            okhttp3.RequestBody
                                .create(MediaType.parse("application/octet-stream"), file.getBytes()))
                        .build();
                Response resp = client.newCall(
                        new Request.Builder()
                            .url(externalCheckCertificateUrl)
                            .post(requestBody).build()).execute();

                if (resp.isSuccessful() && resp.body() != null) {
                    String resString = resp.body().string();
                    if (StringUtils.hasText(resString)) {
                        res = CertificateStatus.valueOf(resString);
                    }
                } else if (resp.code() == HttpStatus.NOT_IMPLEMENTED.value()) {
                    log.warn("controllo della revoca disabilitato");
                    res = CertificateStatus.GOOD;
                } else {
                    String errorMessage = String.format("errore della servlet di controllo del certificato, codice http %s", resp.code());
                    log.error(errorMessage);
                    // se nella risposta c'è del testo lo stampo a log
                    if (resp.body() != null) {
                       String error = resp.body().string();
                       log.error(String.format("la servlet ha tornato %s", error));
                    }
                        
                    throw new FirmaJnJException(errorMessage);
                }
            }
        } catch (Exception ex) {
            log.error(String.format("errore nel controllo del certificato, lo imposto a %s", res.toString()), ex);
        }
        return res.toString();
    }
    
    private CertificateStatus getOfflineCertificateStatus(byte[] cert) throws IOException {
        X509Certificate x509Certificate = AuthorizationUtilityFunctions.getX509CertificateDEREconded(cert);
        CertificateStatus res = CertificateStatus.UNKNOWN;
        try {
            x509Certificate.checkValidity();
            res = CertificateStatus.GOOD;
            } catch (CertificateExpiredException ex) {
                res = CertificateStatus.EXPIRED;
            } catch (CertificateNotYetValidException ex) {
                res = CertificateStatus.NOT_YET_VALID;
            }
        return res;
    }
    
    private String getFirmaJnJServerUrl(HttpServletRequest request) {
        String hostname = CommonUtils.getHostname(request);
        if (!firmaJnJMappingUrl.startsWith("/")) {
           firmaJnJMappingUrl = "/" + firmaJnJMappingUrl;
        }
        String scheme = request.getScheme();
        String port = "";
        if (request.getServerPort() > 0) {
            port = ":" + request.getServerPort();
        }
        String serverUrl = scheme + "://" + hostname + port + firmaJnJMappingUrl;
        return serverUrl;
    }
}
