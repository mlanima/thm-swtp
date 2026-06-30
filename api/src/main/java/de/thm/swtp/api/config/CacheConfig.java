package de.thm.swtp.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Bean
    public CacheManager cacheManager(ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider) {
        RedisConnectionFactory factory = redisConnectionFactoryProvider.getIfAvailable();
        if (factory != null) {
            try (var connection = factory.getConnection()) {
                connection.ping();
                log.info("Redis available \u2014 using RedisCacheManager");
                return RedisCacheManager.builder(factory)
                        .cacheDefaults(defaultCacheConfig())
                        .withCacheConfiguration("tag-exists", tagExistsCacheConfig())
                        .build();
            } catch (Exception e) {
                log.warn("Redis unreachable \u2014 caching disabled: {}", e.getMessage());
            }
        } else {
            log.warn("No Redis connection factory \u2014 caching disabled");
        }
        return new NoOpCacheManager();
    }

    private RedisCacheConfiguration defaultCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                RedisSerializer.json()));
    }

    private RedisCacheConfiguration tagExistsCacheConfig() {
        return defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .prefixCacheNameWith("tags:");
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache get error for key '{}': {}", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache put error for key '{}': {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache evict error for key '{}': {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache clear error: {}", exception.getMessage());
            }
        };
    }
}
