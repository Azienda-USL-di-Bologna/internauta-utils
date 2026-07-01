package it.bologna.ausl.internauta.utils.masterjobs;

import it.bologna.ausl.internauta.utils.masterjobs.configuration.MasterjobsApplicationConfig;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Registro dei thread che stanno eseguendo un set su questa istanza.
 *
 * Ogni thread esecutore, quando entra nell'esecuzione di un set, si registra qui: viene scritta
 * una chiave Redis "masterjobsWorkingThreadsSet:&lt;uniqueName&gt;" (valore = setId) con un TTL.
 * L'alive checker rinfresca periodicamente il TTL finché il set è in esecuzione; a fine set la
 * chiave viene cancellata. Se l'istanza muore, l'alive checker si ferma e le chiavi scadono da
 * sole: i relativi set risultano quindi "orfani" e possono essere recuperati da regenerateQueue.
 *
 * @author gdm
 */
@Component
public class MasterjobsWorkingThreadsRegistry {
    private static final Logger log = LoggerFactory.getLogger(MasterjobsWorkingThreadsRegistry.class);

    @Autowired
    private MasterjobsApplicationConfig masterjobsApplicationConfig;

    @Autowired
    @Qualifier(value = "redisMaterjobs")
    private RedisTemplate redisTemplate;

    // set in esecuzione su questa istanza: uniqueName del thread esecutore -> setId
    private final Map<String, Long> runningSetsByThread = new ConcurrentHashMap<>();

    /**
     * Registra il set come in esecuzione per il thread indicato e scrive subito la chiave Redis.
     */
    public void register(String uniqueName, Long setId) {
        runningSetsByThread.put(uniqueName, setId);
        writeKey(uniqueName, setId);
    }

    /**
     * De-registra il set (fine esecuzione) e cancella la chiave Redis.
     */
    public void unregister(String uniqueName) {
        runningSetsByThread.remove(uniqueName);
        redisTemplate.delete(buildKey(uniqueName));
    }

    /**
     * Rinfresca il TTL delle chiavi di tutti i set attualmente in esecuzione su questa istanza.
     * Va richiamato periodicamente dall'alive checker.
     */
    public void aliveCheck() {
        for (Map.Entry<String, Long> entry : runningSetsByThread.entrySet()) {
            refreshTtl(entry.getKey());
        }
    }

    /**
     * Torna i setId attualmente vivi (in esecuzione) leggendoli dalle chiavi Redis non scadute.
     */
    public Set<Long> getLiveSetIds() {
        Set<Long> liveSetIds = new HashSet<>();
        // RedisTemplate grezzo: le chiavi arrivano come Object, le converto subito a String
        Set keys = redisTemplate.keys(buildKey("*"));
        if (keys != null) {
            for (Object key : keys) {
                Object value = redisTemplate.opsForValue().get(key);
                // value può essere null se la chiave è scaduta tra keys() e get(): in quel caso non è viva
                if (value != null) {
                    liveSetIds.add(Long.valueOf(value.toString()));
                }
            }
        }
        return liveSetIds;
    }

    /**
     * Torna gli uniqueName dei thread attualmente vivi (utile per non toccare le loro work queue).
     */
    public Set<String> getLiveThreadUniqueNames() {
        Set<String> liveNames = new HashSet<>();
        String prefix = buildKey("");
        // RedisTemplate grezzo: le chiavi arrivano come Object, le converto subito a String
        Set keys = redisTemplate.keys(prefix + "*");
        if (keys != null) {
            for (Object key : keys) {
                liveNames.add(key.toString().substring(prefix.length()));
            }
        }
        return liveNames;
    }

    private void writeKey(String uniqueName, Long setId) {
        redisTemplate.opsForValue().set(
            buildKey(uniqueName), String.valueOf(setId),
            masterjobsApplicationConfig.getWorkingThreadsTtlMillis(), TimeUnit.MILLISECONDS);
    }

    // rinfresca solo il TTL di una chiave già esistente: se il thread si è appena de-registrato
    // (chiave cancellata) l'expire è un no-op e non ricrea la chiave (evita chiavi "fantasma")
    private void refreshTtl(String uniqueName) {
        redisTemplate.expire(
            buildKey(uniqueName),
            masterjobsApplicationConfig.getWorkingThreadsTtlMillis(), TimeUnit.MILLISECONDS);
    }

    private String buildKey(String uniqueName) {
        return masterjobsApplicationConfig.getWorkingThreadsSetName() + ":" + uniqueName;
    }
}
