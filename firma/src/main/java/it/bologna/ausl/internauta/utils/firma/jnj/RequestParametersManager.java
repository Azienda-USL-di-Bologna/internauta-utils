package it.bologna.ausl.internauta.utils.firma.jnj;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querydsl.jpa.impl.JPAQueryFactory;
import it.bologna.ausl.internauta.utils.firma.configuration.FirmaHttpClientConfiguration;
import it.bologna.ausl.internauta.utils.firma.data.exceptions.SignParamsException;
import it.bologna.ausl.internauta.utils.firma.data.jnj.SignParams;
import it.bologna.ausl.internauta.utils.firma.exceptions.FirmaParameterException;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJRequestParameterExpiredException;
import it.bologna.ausl.internauta.utils.firma.jnj.exceptions.FirmaJnJRequestParameterNotFoundException;
import it.bologna.ausl.internauta.utils.firma.repositories.RequestParameterRepository;
import it.bologna.ausl.internauta.utils.firma.utils.CommonUtils;
import it.bologna.ausl.internauta.utils.firma.utils.ConfigParams;
import it.bologna.ausl.model.entities.firma.QRequestParameter;
import it.bologna.ausl.model.entities.firma.RequestParameter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Classe che si occupa della gestione dei RequestParameter
 * @author gdm
 */
@Component
public class RequestParametersManager {
    
    private static Logger logger = LoggerFactory.getLogger(ConfigParams.class);

    @Autowired
    private ConfigParams configParams;
    
    @Autowired
    private RequestParameterRepository requestParameterRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Value("${firma.jnj.mapping.url}")
    private String firmaJnJMappingUrl;
    
    @Value("${firma.jnj.params-expire-seconds}")
    private Integer firmaJnJParamsExpireSeconds;
    
    /**
     * Converte e restituisce i RequestParameter identificati dal token passato in SignParams
     * @param token
     * @param extendedValidity
     * @param request
     * @return
     * @throws FirmaJnJRequestParameterExpiredException
     * @throws FirmaJnJRequestParameterNotFoundException 
     */
    public SignParams getRequestParameters(String token, Boolean extendedValidity, HttpServletRequest request) throws FirmaJnJRequestParameterExpiredException, FirmaJnJRequestParameterNotFoundException {
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
    
    /**
     * Converte i SignParams in RequestParameter e li salva su db 
     * @param signParams
     * @return il token per leggere i RequestParameter
     * @throws SignParamsException 
     */
    public String setRequestParameters(SignParams signParams) throws SignParamsException {
        RequestParameter requestParameter = new RequestParameter();
        String token = UUID.randomUUID().toString();
        requestParameter.setId(token);
        requestParameter.setData(signParams.toMap());
        requestParameter.setExpireOn(ZonedDateTime.now().plusSeconds(firmaJnJParamsExpireSeconds));
        RequestParameter savedRequestParamter = requestParameterRepository.save(requestParameter);
        return token;
    }
    
    /**
     * Cancella i RequestParameter scaduti da almeno "n" ore, dove "n" è un parametro che si chiama firmaJnJRequestParameter nella tabella firma.parameters
     * @param now la data e ora correnti, preferisco farla passare a chi chiama questa funzione
     * @throws FirmaParameterException 
     */
    public void deleteExpiredRequestParamters(ZonedDateTime now) throws FirmaParameterException {
        Map<String, Object> firmaJnJRequestParameter = configParams.getFirmaJnJRequestParameter();
        Integer hourBeforeDelete = (Integer) firmaJnJRequestParameter.get(ConfigParams.FirmaJnJRequestParameterParamsParamsKey.hourBeforeDelete.toString());
        if (hourBeforeDelete != null) {
            logger.info(String.format("deleting expired entries older then %s hours", hourBeforeDelete)); 
            QRequestParameter qRequestParameter = QRequestParameter.requestParameter;
            JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
            long deletedEntries = queryFactory
                .delete(qRequestParameter)
                .where(qRequestParameter.expireOn.lt(now.minusHours(hourBeforeDelete)))
                .execute();
            logger.info(String.format("deleted %s expired entries", deletedEntries));
        } else {
            String errorMessage = String.format("il parametro %s non contiene la chiave %s",ConfigParams.ParameterIds.firmaJnJRequestParameter, ConfigParams.FirmaJnJRequestParameterParamsParamsKey.hourBeforeDelete);
            logger.error(errorMessage);
            throw new FirmaParameterException(errorMessage);
        }
    }
    
    /**
     * genera l'url per base per le chiamate alla firma jnj
     * @param request
     * @return 
     */
    private String getFirmaJnJServerUrl(HttpServletRequest request) {
        String hostname = CommonUtils.getHostname(request);
        String url = firmaJnJMappingUrl;
        if (!url.startsWith("/")) {
           url = "/" + url;
        }
        String scheme = request.getScheme();
        String port = "";
        if (request.getServerPort() > 0) {
            port = ":" + request.getServerPort();
        }
        String serverUrl = scheme + "://" + hostname + port + url;
        return serverUrl;
    }
}
