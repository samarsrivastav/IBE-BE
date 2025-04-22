//package backend.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cache.CacheManager;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.cache.caffeine.CaffeineCacheManager;
//import org.springframework.cache.support.CompositeCacheManager;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.cache.RedisCacheManager;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//import org.springframework.data.redis.core.RedisTemplate;
//
//import java.time.Duration;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//
//import com.fasterxml.jackson.annotation.JsonTypeInfo;
//import com.fasterxml.jackson.annotation.PropertyAccessor;
//import com.fasterxml.jackson.annotation.JsonAutoDetect;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
//import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.github.benmanes.caffeine.cache.Caffeine;
//
//@Configuration
//@EnableCaching
//public class CacheConfig {
//
//    @Value("${spring.redis.host}")
//    private String redisHost;
//
//    @Value("${spring.redis.port}")
//    private int redisPort;
//
//    @Value("${redis.password}")
//    private String redisPassword;
//
//    @Value("${spring.redis.username}")
//    private String redisUsername;
//
//    @Bean
//    public JedisConnectionFactory redisConnectionFactory() {
//        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
//        config.setHostName(redisHost);
//        config.setPort(redisPort);
//        config.setUsername(redisUsername);
//        config.setPassword(redisPassword);
//        return new JedisConnectionFactory(config);
//    }
//
//    @Bean
//    public ObjectMapper redisObjectMapper() {
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        objectMapper.activateDefaultTyping(
//                LaissezFaireSubTypeValidator.instance,
//                DefaultTyping.NON_FINAL,
//                JsonTypeInfo.As.PROPERTY
//        );
//        return objectMapper;
//    }
//
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(JedisConnectionFactory factory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(factory);
//        template.setKeySerializer(new StringRedisSerializer());
//        GenericJackson2JsonRedisSerializer serializer =
//                new GenericJackson2JsonRedisSerializer(redisObjectMapper());
//        template.setValueSerializer(serializer);
//        template.setHashValueSerializer(serializer);
//        template.afterPropertiesSet();
//        return template;
//    }
//
//    @Bean
//    public CacheManager caffeineCacheManager() {
//        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
//        cacheManager.setCaffeine(Caffeine.newBuilder()
//                .expireAfterWrite(Duration.ofMinutes(1)) // TTL for Caffeine = 1 minute
//                .maximumSize(100));
//        cacheManager.setCacheNames(Arrays.asList("propertyCache", "tenantConfigurations","minimumRatesCache"));
//        return cacheManager;
//    }
//
//    @Bean
//    public RedisCacheManager redisCacheManager(JedisConnectionFactory factory) {
//        GenericJackson2JsonRedisSerializer serializer =
//                new GenericJackson2JsonRedisSerializer(redisObjectMapper());
//
//        RedisSerializationContext.SerializationPair<Object> valuePair =
//                RedisSerializationContext.SerializationPair.fromSerializer(serializer);
//        RedisSerializationContext.SerializationPair<String> keyPair =
//                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer());
//
//        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(5)) // Default TTL = 5 minutes
//                .prefixCacheNameWith("ibe:")
//                .serializeKeysWith(keyPair)
//                .serializeValuesWith(valuePair)
//                .disableCachingNullValues();
//
//        Map<String, RedisCacheConfiguration> specificConfigs = new HashMap<>();
//        specificConfigs.put("propertyCache", defaultConfig.entryTtl(Duration.ofMinutes(30))); // TTL = 30 mins
//        specificConfigs.put("tenantConfigurations", defaultConfig.entryTtl(Duration.ofMinutes(15))); // TTL = 15 mins
//        specificConfigs.put("minimumRatesCache", defaultConfig.entryTtl(Duration.ofMinutes(30))); // TTL = 0 mins
//
//        return RedisCacheManager.builder(factory)
//                .cacheDefaults(defaultConfig)
//                .withInitialCacheConfigurations(specificConfigs)
//                .transactionAware()
//                .build();
//    }
//
//    @Primary
//    @Bean
//    public CacheManager compositeCacheManager(
//            CacheManager caffeineCacheManager,
//            CacheManager redisCacheManager) {
//        CompositeCacheManager compositeCacheManager = new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
//        compositeCacheManager.setFallbackToNoOpCache(false); // fallback to next if not found
//        return compositeCacheManager;
//    }
