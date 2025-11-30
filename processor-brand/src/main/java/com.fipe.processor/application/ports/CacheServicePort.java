package com.fipe.processor.application.ports;

import java.time.Duration;
import reactor.core.publisher.Mono;

public interface CacheServicePort {

    <T> Mono<T> get(String key, Class<T> valueType);

    <T> Mono<Void> put(String key, T value, Duration ttl);

    Mono<Void> delete(String key);

    Mono<Void> deleteByPattern(String pattern);
}
