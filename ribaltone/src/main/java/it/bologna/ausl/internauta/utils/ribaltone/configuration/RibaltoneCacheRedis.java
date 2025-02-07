package it.bologna.ausl.internauta.utils.ribaltone.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.DatiRibaltoneInterface;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operation;
import it.bologna.ausl.internauta.utils.ribaltone.basedata.Operations;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAnagrafica;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationAppartenente;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationStruttura;
import it.bologna.ausl.internauta.utils.ribaltone.operation.OperationTrasformazione;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 *
 * @author Top
 */
public class RibaltoneCacheRedis extends RibaltoneCache {
    
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RibaltoneCacheRedis(ObjectMapper objectMapper, Map<String, Object> cacheConfig) {
        this.objectMapper = objectMapper;
        redisTemplate = this.buildRedisTemplate(cacheConfig);
    }
    
    @Override
    public void dump(Operations operations, String key) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    
    @Override
    public Operations restore(String key) throws ClassNotFoundException {
        List<OperationStruttura> listOfOperationStrutture = new ArrayList();
        List<OperationAppartenente> listOfOperationAppartenenti = new ArrayList();
        List<OperationAnagrafica> listOfOperationAnagrafiche = new ArrayList();
        List<OperationTrasformazione> listOfOperationTrasformazioni = new ArrayList();
        
        
        //da qui il dato (NON PENSO SIA GIUSTO)
        List<Map<String, Object>> data = this.getData(key);
        
        for (Map<String, Object> operationDaRedis : data) {
            Map<String, Object> entitaCoinvoltaMap = (Map<String, Object>) operationDaRedis.get("entitaCoinvolta");
            String entitaName = (String) entitaCoinvoltaMap.get("classe");
            Class<DatiRibaltoneInterface> c = (Class<DatiRibaltoneInterface>) Class.forName(entitaName);
            DatiRibaltoneInterface entitaCoinvolta = this.objectMapper.convertValue(entitaCoinvoltaMap, c);

            switch (operationDaRedis.get("tipo").toString()) {
                case "Anagrafica":
                    listOfOperationAnagrafiche.add(new OperationAnagrafica(Operation.Azione.valueOf(operationDaRedis.get("azione").toString()), entitaCoinvolta));
                    break;
                case "Appartenente":
                    listOfOperationAppartenenti.add(new OperationAppartenente(Operation.Azione.valueOf(operationDaRedis.get("azione").toString()), entitaCoinvolta));
                    break;
                case "Struttura":
                    listOfOperationStrutture.add(new OperationStruttura(Operation.Azione.valueOf(operationDaRedis.get("azione").toString()), entitaCoinvolta));
                    break;
                case "Trasformazione":
                    listOfOperationTrasformazioni.add(new OperationTrasformazione(Operation.Azione.valueOf(operationDaRedis.get("azione").toString()), entitaCoinvolta));
                    break;
            }
        }

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
            if (passwordOBJ != null && !passwordOBJ.toString().equals("") ) {
                redisStandaloneConfig.setPassword(RedisPassword.of(passwordOBJ.toString()));
            }

            // Creiamo una nuova connessione
            LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisStandaloneConfig);
            connectionFactory.afterPropertiesSet();

            // Cambiamo la connessione nel RedisTemplate
            redisTemplate.setConnectionFactory(connectionFactory);
            redisTemplate.afterPropertiesSet();

            return redisTemplate; // Connessione cambiata con successo
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public void saveData(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public List<Map<String, Object>> getData(String key) {
        return (List<Map<String, Object>>) redisTemplate.opsForValue().get(key);
    }
    
}
