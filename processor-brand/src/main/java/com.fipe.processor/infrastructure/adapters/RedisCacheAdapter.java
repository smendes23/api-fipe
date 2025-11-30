package com.fipe.processor.infrastructure.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipe.processor.application.ports.CacheServicePort;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class RedisCacheAdapter implements CacheServicePort {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheAdapter(@Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Mono<T> get(final String key, final Class<T> valueType) {
        log.debug("Getting value from cache: {}", key);

        return redisTemplate.opsForValue()
                .get(key)
                .flatMap(value -> deserialize(value, valueType))
                .doOnSuccess(value -> {
                    if (value != null) {
                        log.debug("Cache hit: {}", key);
                    } else {
                        log.debug("Cache miss: {}", key);
                    }
                })
                .doOnError(error -> log.error("Error getting value from cache {}: {}",
                        key, error.getMessage()));
    }

    @Override
    public <T> Mono<Void> put(final String key, final T value, Duration ttl) {
        log.debug("Putting value in cache: {} with TTL: {}", key, ttl);

        return serialize(value)
                .flatMap(serialized -> redisTemplate.opsForValue()
                        .set(key, serialized, ttl))
                .then()
                .doOnSuccess(v -> log.debug("Value cached successfully: {}", key))
                .doOnError(error -> log.error("Error putting value in cache {}: {}",
                        key, error.getMessage()));
    }

    @Override
    public Mono<Void> delete(final String key) {
        log.debug("Deleting value from cache: {}", key);

        return redisTemplate.delete(key)
                .then()
                .doOnSuccess(v -> log.debug("Value deleted from cache: {}", key))
                .doOnError(error -> log.error("Error deleting value from cache {}: {}",
                        key, error.getMessage()));
    }

    @Override
    public Mono<Void> deleteByPattern(final String pattern) {
        log.debug("Deleting values from cache by pattern: {}", pattern);

        return redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .then()
                .doOnSuccess(v -> log.debug("Values deleted from cache by pattern: {}", pattern))
                .doOnError(error -> log.error("Error deleting values from cache by pattern {}: {}",
                        pattern, error.getMessage()));
    }

    private <T> Mono<String> serialize(final T value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return Mono.just(json);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Error serializing value", e));
        }
    }

    private <T> Mono<T> deserialize(final String json, final Class<T> valueType) {
        try {
            T value = objectMapper.readValue(json, valueType);
            return Mono.just(value);
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Error deserializing value", e));
        }
    }
}

