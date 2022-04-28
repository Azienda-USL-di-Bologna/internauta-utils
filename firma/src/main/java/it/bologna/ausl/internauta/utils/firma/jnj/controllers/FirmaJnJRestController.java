package it.bologna.ausl.internauta.utils.firma.jnj.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.firma.data.exceptions.SignParamsException;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParamsComponent;
import it.bologna.ausl.internauta.utils.firma.remota.controllers.*;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemota;
import it.bologna.ausl.internauta.utils.firma.remota.FirmaRemotaFactory;
import it.bologna.ausl.internauta.utils.firma.data.remota.FirmaRemotaInformation;
import it.bologna.ausl.internauta.utils.firma.data.remota.UserInformation;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJException;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJRequestParameterExpiredException;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJRequestParameterNotFoundException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.FirmaRemotaConfigurationException;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.ControllerHandledExceptions;
import it.bologna.ausl.internauta.utils.firma.remota.exceptions.http.FirmaRemotaHttpException;
import it.bologna.ausl.internauta.utils.firma.repositories.RequestParameterRepository;
import it.bologna.ausl.internauta.utils.firma.utils.CommonUtils;
import it.bologna.ausl.model.entities.firma.RequestParameter;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private RequestParameterRepository requestParameterRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${firma.jnj.mapping.url}")
    private String firmaJnJMappingUrl;
    
    @Value("${firma.jnj.params-expire-seconds}")
    private Integer firmaJnJParamsExpireSeconds;

    @RequestMapping(value = "/test/{nome}/{cognome}", method = RequestMethod.GET)
    public String test(@PathVariable String nome, @PathVariable String cognome) {

//        System.out.println("ESITO: " + var);
        return "ciao " + nome + cognome;
    }
    
    @RequestMapping(value = "/getParameters", method = RequestMethod.GET)
    public SignParams getParameters(@RequestParam(required = true) String token, HttpServletRequest request) throws FirmaJnJException {
        Optional<RequestParameter> requestParamOptional = requestParameterRepository.findById(token);
        
        SignParams res = null;
        if (requestParamOptional.isPresent()) {
            RequestParameter requestParameter = requestParamOptional.get();
            if (requestParameter.getExpireOn().isAfter(ZonedDateTime.now())) {
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
