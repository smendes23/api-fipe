package com.fipe.processor.infrastructure.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisCacheAdapterTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RedisCacheAdapter redisCacheAdapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should get value from cache successfully")
    void shouldGetValueFromCacheSuccessfully() throws JsonProcessingException {
        String key = "test:key";
        String cachedJson = "{\"id\":\"1\",\"name\":\"Test\"}";
        TestObject expectedObject = new TestObject("1", "Test");

        when(valueOperations.get(key)).thenReturn(Mono.just(cachedJson));
        when(objectMapper.readValue(cachedJson, TestObject.class)).thenReturn(expectedObject);

        Mono<TestObject> result = redisCacheAdapter.get(key, TestObject.class);

        StepVerifier.create(result)
                .expectNext(expectedObject)
                .verifyComplete();

        verify(valueOperations, times(1)).get(key);
        verify(objectMapper, times(1)).readValue(cachedJson, TestObject.class);
    }

    @Test
    @DisplayName("Should return empty Mono when key not found in cache")
    void shouldReturnEmptyMonoWhenKeyNotFound() {
        String key = "test:key";

        when(valueOperations.get(key)).thenReturn(Mono.empty());

        Mono<TestObject> result = redisCacheAdapter.get(key, TestObject.class);

        StepVerifier.create(result)
                .verifyComplete();

        verify(valueOperations, times(1)).get(key);
    }

    @Test
    @DisplayName("Should handle deserialization error")
    void shouldHandleDeserializationError() throws JsonProcessingException {
        String key = "test:key";
        String cachedJson = "invalid-json";
        JsonProcessingException jsonException = new JsonProcessingException("Invalid JSON") {};

        when(valueOperations.get(key)).thenReturn(Mono.just(cachedJson));
        when(objectMapper.readValue(cachedJson, TestObject.class)).thenThrow(jsonException);

        Mono<TestObject> result = redisCacheAdapter.get(key, TestObject.class);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Error deserializing value") &&
                                throwable.getCause() == jsonException)
                .verify();

        verify(valueOperations, times(1)).get(key);
        verify(objectMapper, times(1)).readValue(cachedJson, TestObject.class);
    }

    @Test
    @DisplayName("Should put value in cache successfully")
    void shouldPutValueInCacheSuccessfully() throws JsonProcessingException {
        String key = "test:key";
        TestObject value = new TestObject("1", "Test");
        Duration ttl = Duration.ofMinutes(30);
        String serializedJson = "{\"id\":\"1\",\"name\":\"Test\"}";

        when(objectMapper.writeValueAsString(value)).thenReturn(serializedJson);
        when(valueOperations.set(key, serializedJson, ttl)).thenReturn(Mono.just(true));

        Mono<Void> result = redisCacheAdapter.put(key, value, ttl);

        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper, times(1)).writeValueAsString(value);
        verify(valueOperations, times(1)).set(key, serializedJson, ttl);
    }

    @Test
    @DisplayName("Should handle serialization error when putting value")
    void shouldHandleSerializationErrorWhenPuttingValue() throws JsonProcessingException {
        String key = "test:key";
        TestObject value = new TestObject("1", "Test");
        Duration ttl = Duration.ofMinutes(30);
        JsonProcessingException jsonException = new JsonProcessingException("Serialization error") {};

        when(objectMapper.writeValueAsString(value)).thenThrow(jsonException);

        Mono<Void> result = redisCacheAdapter.put(key, value, ttl);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Error serializing value") &&
                                throwable.getCause() == jsonException)
                .verify();

        verify(objectMapper, times(1)).writeValueAsString(value);
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Should handle Redis error when putting value")
    void shouldHandleRedisErrorWhenPuttingValue() throws JsonProcessingException {
        String key = "test:key";
        TestObject value = new TestObject("1", "Test");
        Duration ttl = Duration.ofMinutes(30);
        String serializedJson = "{\"id\":\"1\",\"name\":\"Test\"}";
        RuntimeException redisError = new RuntimeException("Redis connection failed");

        when(objectMapper.writeValueAsString(value)).thenReturn(serializedJson);
        when(valueOperations.set(key, serializedJson, ttl)).thenReturn(Mono.error(redisError));

        Mono<Void> result = redisCacheAdapter.put(key, value, ttl);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(objectMapper, times(1)).writeValueAsString(value);
        verify(valueOperations, times(1)).set(key, serializedJson, ttl);
    }

    @Test
    @DisplayName("Should delete key successfully")
    void shouldDeleteKeySuccessfully() {
        String key = "test:key";

        when(redisTemplate.delete(key)).thenReturn(Mono.just(1L));

        Mono<Void> result = redisCacheAdapter.delete(key);

        StepVerifier.create(result)
                .verifyComplete();

        verify(redisTemplate, times(1)).delete(key);
    }

    @Test
    @DisplayName("Should handle Redis error when deleting key")
    void shouldHandleRedisErrorWhenDeletingKey() {
        String key = "test:key";
        RuntimeException redisError = new RuntimeException("Redis connection failed");

        when(redisTemplate.delete(key)).thenReturn(Mono.error(redisError));

        Mono<Void> result = redisCacheAdapter.delete(key);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(redisTemplate, times(1)).delete(key);
    }

    @Test
    @DisplayName("Should delete by pattern successfully")
    void shouldDeleteByPatternSuccessfully() {
        String pattern = "test:*";
        String key1 = "test:1";
        String key2 = "test:2";

        when(redisTemplate.keys(pattern)).thenReturn(Flux.just(key1, key2));
        when(redisTemplate.delete(key1)).thenReturn(Mono.just(1L));
        when(redisTemplate.delete(key2)).thenReturn(Mono.just(1L));

        Mono<Void> result = redisCacheAdapter.deleteByPattern(pattern);

        StepVerifier.create(result)
                .verifyComplete();

        verify(redisTemplate, times(1)).keys(pattern);
        verify(redisTemplate, times(1)).delete(key1);
        verify(redisTemplate, times(1)).delete(key2);
    }

    @Test
    @DisplayName("Should handle empty keys when deleting by pattern")
    void shouldHandleEmptyKeysWhenDeletingByPattern() {
        String pattern = "test:*";

        when(redisTemplate.keys(pattern)).thenReturn(Flux.empty());

        Mono<Void> result = redisCacheAdapter.deleteByPattern(pattern);

        StepVerifier.create(result)
                .verifyComplete();

        verify(redisTemplate, times(1)).keys(pattern);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should handle Redis error when deleting by pattern")
    void shouldHandleRedisErrorWhenDeletingByPattern() {
        String pattern = "test:*";
        String key = "test:1";
        RuntimeException redisError = new RuntimeException("Redis connection failed");

        when(redisTemplate.keys(pattern)).thenReturn(Flux.just(key));
        when(redisTemplate.delete(key)).thenReturn(Mono.error(redisError));

        Mono<Void> result = redisCacheAdapter.deleteByPattern(pattern);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(redisTemplate, times(1)).keys(pattern);
        verify(redisTemplate, times(1)).delete(key);
    }

    @Test
    @DisplayName("Should handle null value when getting from cache")
    void shouldHandleNullValueWhenGettingFromCache() {
        String key = "test:key";

        when(valueOperations.get(key)).thenReturn(Mono.empty());

        Mono<TestObject> result = redisCacheAdapter.get(key, TestObject.class);

        StepVerifier.create(result)
                .verifyComplete();

        verify(valueOperations, times(1)).get(key);
    }

    @Test
    @DisplayName("Should put null value in cache")
    void shouldPutNullValueInCache() throws JsonProcessingException {
        String key = "test:key";
        Duration ttl = Duration.ofMinutes(30);
        String serializedJson = "null";

        when(objectMapper.writeValueAsString(null)).thenReturn(serializedJson);
        when(valueOperations.set(key, serializedJson, ttl)).thenReturn(Mono.just(true));

        Mono<Void> result = redisCacheAdapter.put(key, null, ttl);

        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper, times(1)).writeValueAsString(null);
        verify(valueOperations, times(1)).set(key, serializedJson, ttl);
    }

    @Test
    @DisplayName("Should handle complex object serialization")
    void shouldHandleComplexObjectSerialization() throws JsonProcessingException {
        String key = "test:key";
        ComplexObject complexObject = new ComplexObject("parent", new TestObject("1", "child"));
        Duration ttl = Duration.ofMinutes(30);
        String serializedJson = "{\"parentName\":\"parent\",\"child\":{\"id\":\"1\",\"name\":\"child\"}}";

        when(objectMapper.writeValueAsString(complexObject)).thenReturn(serializedJson);
        when(valueOperations.set(key, serializedJson, ttl)).thenReturn(Mono.just(true));

        Mono<Void> result = redisCacheAdapter.put(key, complexObject, ttl);

        StepVerifier.create(result)
                .verifyComplete();

        verify(objectMapper, times(1)).writeValueAsString(complexObject);
        verify(valueOperations, times(1)).set(key, serializedJson, ttl);
    }

    static class TestObject {
        private String id;
        private String name;

        public TestObject(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject that = (TestObject) o;
            return java.util.Objects.equals(id, that.id) &&
                    java.util.Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name);
        }
    }

    static class ComplexObject {
        private String parentName;
        private TestObject child;

        public ComplexObject(String parentName, TestObject child) {
            this.parentName = parentName;
            this.child = child;
        }

        public String getParentName() { return parentName; }
        public TestObject getChild() { return child; }
    }
}