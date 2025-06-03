///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
// */
//package it.bologna.ausl.internauta.utils.ribaltone.cache;
//
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.core.RedisTemplate;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneCacheConfig;
//import it.bologna.ausl.internauta.utils.ribaltone.configuration.RibaltoneCacheRedisConfig;
//import java.util.Map;
//import org.springframework.data.redis.connection.RedisPassword;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
///**
// *
// * @author Top
// */
//
//
//public class RedisConfiguration {
//    
//    private RibaltoneCacheConfig redisConfig;
//    private ObjectMapper objectMapper;
//    private Map<String, RedisTemplate<String, Object>> redisTemplateMap;
//
//    public boolean switchRedisInstance(RibaltoneCacheRedisConfig redisConfig, String codiceAzienda) {
//        
//         
//        if (redisConfig == null) {
//            return false; // Configurazione non trovata
//        }
//
//        try {
//            // Creiamo una nuova configurazione Redis dinamicamente
//            RedisStandaloneConfiguration redisStandaloneConfig = new RedisStandaloneConfiguration(redisConfig.getHost(), redisConfig.getPort());
//            redisStandaloneConfig.setDatabase(redisConfig.getDb());
//            if (redisConfig.getPassword() != null && !redisConfig.getPassword().isEmpty()) {
//                redisStandaloneConfig.setPassword(RedisPassword.of(redisConfig.getPassword()));
//            }
//
//            // Creiamo una nuova connessione
//            LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisStandaloneConfig);
//            connectionFactory.afterPropertiesSet();
//
//            // Cambiamo la connessione nel RedisTemplate
//            redisTemplate.setConnectionFactory(connectionFactory);
//            redisTemplate.afterPropertiesSet();
//
//            return true; // Connessione cambiata con successo
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    
//}
