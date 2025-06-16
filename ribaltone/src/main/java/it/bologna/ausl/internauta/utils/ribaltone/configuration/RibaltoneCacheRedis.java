package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.authorizationutils.session.AuthenticatedSessionData;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface.TipologiaCsv;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAnagrafica;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationTrasformazione;
import it.bologna.ausl.internauta.utils.ribaltone.plugin.csv.CsvImportManager;
import it.bologna.ausl.model.entities.baborg.Utente;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 *
 * @author Top
 */
public class RibaltoneCacheRedis extends RibaltoneCache {

    private static final Logger log = LoggerFactory.getLogger(RibaltoneCacheRedis.class);

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Integer timeToExpire;
    private final String keyRibaltoneDati;
    private final String keyImportingCSV;
    private final String keyExecuting;
    private final EntityManager entityManager;

    public RibaltoneCacheRedis(ObjectMapper objectMapper, Map<String, Object> cacheConfig, EntityManager entityManager) {
        this.objectMapper = objectMapper;
        redisTemplate = this.buildRedisTemplate(cacheConfig);
        this.timeToExpire = Integer.valueOf(cacheConfig.get("cacheTime").toString());
        this.keyRibaltoneDati = "RIBALTONE_Dati_" + cacheConfig.get("codiceAzienda").toString();
        this.keyImportingCSV = "RIBALTONE_ImportingCSV_" + cacheConfig.get("codiceAzienda").toString();
        //vogliamo che vada un solo ribaltone alla volta
        this.keyExecuting = "RIBALTONE_KeyExecuting";
        this.entityManager = entityManager;
    }

    @Override
    public void dump(Operations operations) throws RibaltoneHttpException {

        try {
            String jsonOperation = objectMapper.writeValueAsString(operations);
            this.saveData(keyRibaltoneDati, jsonOperation);
        } catch (JsonProcessingException ex) {
            throw new RibaltoneHttpException("Errore nella serializzazione JSON", ex);
        }
    }

    @Override
    public Operations restore() throws ClassNotFoundException, RibaltoneHttpException, JsonProcessingException {
        List<OperationStruttura> listOfOperationStrutture = new ArrayList();
        List<OperationAppartenente> listOfOperationAppartenenti = new ArrayList();
        List<OperationAnagrafica> listOfOperationAnagrafiche = new ArrayList();
        List<OperationTrasformazione> listOfOperationTrasformazioni = new ArrayList();

        Map<String, List<Map<String, Object>>> data = this.getData(keyRibaltoneDati);
        if (data == null) {
            return null;
        }
        for (String key : data.keySet()) {
//            List<Map<String, Object>> op = objectMapper.readValue(data.get(key).toString(),
//                    new TypeReference<List<Map<String,Object>>>(){}
//            );
            for (Map<String, Object> operationDaRedis : data.get(key)) {
//
                Map<String, Object> entitaCoinvoltaMap = (Map<String, Object>) operationDaRedis.get("entitaCoinvolta");
                String entitaName = (String) entitaCoinvoltaMap.get("classe");
                Class<DatiRibaltoneInterface> c = (Class<DatiRibaltoneInterface>) Class.forName(entitaName);
                DatiRibaltoneInterface entitaCoinvolta = this.objectMapper.convertValue(entitaCoinvoltaMap, c);

                switch (operationDaRedis.get("tipo").toString()) {
                    case "ANAGRAFICHE" ->
                        listOfOperationAnagrafiche.add(new OperationAnagrafica(Operation.Azione.valueOf(operationDaRedis.get("azione").toString()), entitaCoinvolta, entityManager));
                    case "APPARTENENTI" -> {
                        List<String> edit = (List<String>) operationDaRedis.get("listOfEdit");
                        listOfOperationAppartenenti.add(new OperationAppartenente(Operation.Azione.valueOf(operationDaRedis.get("azione").toString()), entitaCoinvolta, entityManager, edit, operationDaRedis.get("nomeCasella").toString()));
                    }
                    case "STRUTTURE" ->
                        listOfOperationStrutture.add(new OperationStruttura(Operation.Azione.valueOf(operationDaRedis.get("azione").toString()), entitaCoinvolta, entityManager));
                    case "TRASFORMAZIONI" ->
                        listOfOperationTrasformazioni.add(new OperationTrasformazione(Operation.Azione.valueOf(operationDaRedis.get("azione").toString()), entitaCoinvolta, entityManager));
                }
            }
        }
//
        return new Operations(listOfOperationStrutture, listOfOperationAppartenenti, listOfOperationAnagrafiche, listOfOperationTrasformazioni);
    }

    private RedisTemplate<String, Object> buildRedisTemplate(Map<String, Object> cacheConfig) {
        if (cacheConfig == null) {
            return null; // Configurazione non trovata
        }

        try {
            // Creiamo una nuova configurazione Redis dinamicamente
            RedisStandaloneConfiguration redisStandaloneConfig = new RedisStandaloneConfiguration(cacheConfig.get("host").toString(), Integer.parseInt(cacheConfig.get("port").toString()));
            redisStandaloneConfig.setDatabase((int) cacheConfig.get("db"));
            Object passwordOBJ = cacheConfig.get("password");
            if (passwordOBJ != null && !passwordOBJ.toString().equals("")) {
                redisStandaloneConfig.setPassword(RedisPassword.of(passwordOBJ.toString()));
            }

            // Creiamo una nuova connessione
            LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisStandaloneConfig);
            connectionFactory.afterPropertiesSet();
            RedisTemplate<String, Object> redisTemp = new RedisTemplate<>();
            // Cambiamo la connessione nel RedisTemplate
            redisTemp.setConnectionFactory(connectionFactory);
            redisTemp.afterPropertiesSet();
            redisTemp.setKeySerializer(new StringRedisSerializer());
            redisTemp.setValueSerializer(new GenericJackson2JsonRedisSerializer());
            redisTemp.afterPropertiesSet();
            return redisTemp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveData(String key, String value) {
        redisTemplate.opsForValue().set(key, value, this.timeToExpire, TimeUnit.MINUTES);
    }

    public Map<String, List<Map<String, Object>>> getData(String key) throws RibaltoneHttpException, JsonProcessingException {

        if (redisTemplate.opsForValue().get(key) == null) {
            return null;
        }
        Map<String, List<Map<String, Object>>> op = objectMapper.readValue((String) redisTemplate.opsForValue().get(key),
            new TypeReference<Map<String, List<Map<String, Object>>>>() {
        }
        );
        if (op == null || op.isEmpty()) {
            throw new RibaltoneHttpException("errore nel reperire le operations dalla cache");
        }
        return op;
    }

    @Override
    public void cleanDataCache() {
        Set<String> keys = redisTemplate.keys(keyRibaltoneDati + "*");
        redisTemplate.delete(keys);
    }

    @Override
    public Boolean isExecuting() {
        return redisTemplate.hasKey(keyExecuting);
    }

    @Override
    public void setExecuting(Boolean executing, Utente user) throws JsonProcessingException {
        if (executing) {
            String userStr = objectMapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(keyExecuting, userStr, 60, TimeUnit.MINUTES);
        } else {
            redisTemplate.delete(keyExecuting);
        }
    }

    @Override
    public Boolean isImportingCSV() {
        return redisTemplate.hasKey(keyImportingCSV);
    }

    @Override
    public void setImportingCSV(Boolean executing, Utente user) throws JsonProcessingException {
        if (executing) {
            redisTemplate.opsForValue().set(keyImportingCSV, user.getId().toString(), 60, TimeUnit.MINUTES);
        } else {
            redisTemplate.delete(keyImportingCSV);
        }
    }

    @Override
    public Integer getIdUserExecuting() throws RibaltoneHttpException, JsonProcessingException {
        if (redisTemplate.opsForValue().get(keyExecuting) == null) {
            return null;
        }
        Integer idUtente = objectMapper.readValue((String) redisTemplate.opsForValue().get(keyExecuting), new TypeReference<Integer>() {
        });
        if (idUtente == null) {
            throw new RibaltoneHttpException("errore nel reperire le l'utente che ha inizato il ribaltone dalla cache");
        }

        return idUtente;
    }

    @Override
    public Integer getIdUserImportingCSV() throws JsonProcessingException, RibaltoneHttpException {
        if (redisTemplate.opsForValue().get(keyImportingCSV) == null) {
            return null;
        }
        Integer idUtente = objectMapper.readValue((String) redisTemplate.opsForValue().get(keyImportingCSV), new TypeReference<Integer>() {
        });
        if (idUtente == null) {
            throw new RibaltoneHttpException("errore nel reperire le l'utente che ha inizato il ribaltone dalla cache");
        }
        return idUtente;
    }
}
