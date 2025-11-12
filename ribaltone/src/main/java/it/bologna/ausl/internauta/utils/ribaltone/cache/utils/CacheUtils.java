/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package it.bologna.ausl.internauta.utils.ribaltone.cache.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils;
import static it.bologna.ausl.internauta.utils.ribaltone.RibaltoneManagerUtils.getRibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.cache.RibaltoneCache;
import it.bologna.ausl.internauta.utils.ribaltone.exceptions.http.RibaltoneHttpException;
import it.bologna.ausl.internauta.utils.ribaltone.repository.RepositoryFactory;
import it.bologna.ausl.model.entities.baborg.Utente;
import it.bologna.ausl.model.entities.ribaltonedati.RibaltoneDataConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author Top
 */
public class CacheUtils {

    public static boolean isRibaltoneInCorsoFromCache(String idSelectedConfiguration, RepositoryFactory repositoryFactory, ObjectMapper objectMapper, TransactionTemplate transactionTemplate) throws RibaltoneHttpException {
        return transactionTemplate.execute(action -> {
            RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), idSelectedConfiguration);
            RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
            return ribaltoneCache.isExecuting();
        });
    }

    public static void setRibaltoneCacheInCorso(String idSelectedConfiguration, Utente utente, RepositoryFactory repositoryFactory, ObjectMapper objectMapper, TransactionTemplate transactionTemplate) throws RibaltoneHttpException, JsonProcessingException {
        transactionTemplate.executeWithoutResult(action -> {
            RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), idSelectedConfiguration);
            RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
            ribaltoneCache.setExecuting(Boolean.TRUE, utente);
        });
    }

    public static void setRibaltoneCacheFinito(String idSelectedConfiguration, RepositoryFactory repositoryFactory, ObjectMapper objectMapper, TransactionTemplate transactionTemplate) throws RibaltoneHttpException, JsonProcessingException {
        transactionTemplate.executeWithoutResult(action -> {
            RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), idSelectedConfiguration);
            RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
            ribaltoneCache.setExecuting(Boolean.FALSE, null);
        });
    }

    public static boolean isImportazioneCSVInCorsoFromCache(String idSelectedConfiguration, RepositoryFactory repositoryFactory, ObjectMapper objectMapper, TransactionTemplate transactionTemplate) throws RibaltoneHttpException {
        return transactionTemplate.execute(action -> {
            RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), idSelectedConfiguration);
            RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
            return ribaltoneCache.isImportingCSV();
        });
    }

    public static void setImportazioneCSVInCorso(String idSelectedConfiguration, Utente utente, RepositoryFactory repositoryFactory, ObjectMapper objectMapper, TransactionTemplate transactionTemplate) throws RibaltoneHttpException, JsonProcessingException {
        transactionTemplate.executeWithoutResult(action -> {
            RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), idSelectedConfiguration);
            RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
            ribaltoneCache.setImportingCSV(true, utente);
        });
    }

    public static void setImportazioneCSVFinito(String idSelectedConfiguration, Utente utente, RepositoryFactory repositoryFactory, ObjectMapper objectMapper, TransactionTemplate transactionTemplate) throws RibaltoneHttpException, JsonProcessingException {
        transactionTemplate.executeWithoutResult(action -> {
            RibaltoneDataConfiguration ribaltoneConf = RibaltoneManagerUtils.getRibaltoneConf(repositoryFactory.getEntityManager(), idSelectedConfiguration);
            RibaltoneCache ribaltoneCache = getRibaltoneCache(objectMapper, ribaltoneConf.getCacheConfig(), repositoryFactory.getEntityManager());
            ribaltoneCache.setImportingCSV(false, utente);
        });
    }
}
