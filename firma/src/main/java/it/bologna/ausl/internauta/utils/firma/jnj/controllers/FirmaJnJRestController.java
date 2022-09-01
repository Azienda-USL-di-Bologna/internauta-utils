package it.bologna.ausl.internauta.utils.firma.jnj.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.firma.data.exceptions.SignParamsException;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJException;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJRequestParameterExpiredException;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJRequestParameterNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.firma.repositories.RequestParameterRepository;
import it.bologna.ausl.internauta.utils.firma.utils.CommonUtils;
import it.bologna.ausl.model.entities.firma.RequestParameter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    private RequestParameterRepository requestParameterRepository;
    
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
