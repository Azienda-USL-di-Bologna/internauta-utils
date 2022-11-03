package it.bologna.ausl.internauta.utils.masterjobs.configuration;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 * @author gdm
 */
@Configuration
public class MasterjobsRedisConfig {
    
    @Value("${masterjobs.redis.host}")
    private String masterjobsRedisHost;
    @Value("${masterjobs.redis.port:6379}")
    private Integer masterjobsRedisPort;
    @Value("${masterjobs.redis.db:3}")
    private Integer masterjobsRedisDb;
    @Value("${masterjobs.redis.timeout-millis}")
    private Integer masterjobsRedisTimeoutMillis;

    
    public RedisConnectionFactory masterjobsJedisConnectionFactory() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setBlockWhenExhausted(false);

        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(masterjobsRedisHost, masterjobsRedisPort);
        redisStandaloneConfiguration.setDatabase(masterjobsRedisDb);
        JedisClientConfiguration jedisClientConfiguration = JedisClientConfiguration
            .builder()
            .connectTimeout(Duration.ofMillis(masterjobsRedisTimeoutMillis))
            .readTimeout(Duration.ofMillis(masterjobsRedisTimeoutMillis))
            .usePooling()
            .poolConfig(poolConfig)
            .build();

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration);
        jedisConnectionFactory.afterPropertiesSet();
        return jedisConnectionFactory;
    }
    
    @Bean(name = "redisMaterjobs")
    public RedisTemplate<String, Object> materjobsRedis() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(masterjobsJedisConnectionFactory());
//        RedisSerializer<Object> defaultSerializer = new JdkSerializationRedisSerializer(getClass().getClassLoader());
        RedisSerializer<Object> jacksonSerializer = new GenericJackson2JsonRedisSerializer();
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setDefaultSerializer(stringRedisSerializer);
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(stringRedisSerializer);
        template.setValueSerializer(stringRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
