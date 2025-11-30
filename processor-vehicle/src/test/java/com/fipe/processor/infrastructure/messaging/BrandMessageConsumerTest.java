package com.fipe.processor.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipe.processor.application.usecases.ProcessBrandUseCase;
import com.fipe.processor.domain.MessageProcessingException;
import com.fipe.processor.infrastructure.adapters.input.kafka.BrandMessageConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import java.io.IOException;
import org.apache.kafka.common.errors.TimeoutException;
import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.Disposable;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BrandMessageConsumerTest {

    @Mock
    private KafkaReceiver<String, String> kafkaReceiver;

    @Mock
    private ProcessBrandUseCase processBrandUseCase;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RateLimiterRegistry rateLimiterRegistry;

    @Mock
    private RateLimiter rateLimiter;

    @Mock
    private ReceiverRecord<String, String> receiverRecord;

    @Mock
    private ReceiverOffset receiverOffset;

    private BrandMessageConsumer brandMessageConsumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        brandMessageConsumer = new BrandMessageConsumer(
                kafkaReceiver, processBrandUseCase, objectMapper, rateLimiterRegistry
        );

        // Configurar valores dos campos via reflection para testes
        setField(brandMessageConsumer, "brandsTopic", "brands-topic");
        setField(brandMessageConsumer, "fipeRateLimiter", rateLimiter);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void isRetryableError_ShouldReturnTrueForRetryableErrors() {
        assertThat(invokeIsRetryableError(new TimeoutException())).isTrue();

        assertThat(invokeIsRetryableError(new IOException())).isTrue();

        BrandMessageConsumer consumer = new BrandMessageConsumer(kafkaReceiver, processBrandUseCase, objectMapper, rateLimiterRegistry);
        MessageProcessingException retryableException = new MessageProcessingException("Error", new RuntimeException(), true);
        assertThat(invokeIsRetryableError(retryableException)).isTrue();
    }

    @Test
    void isRetryableError_ShouldReturnFalseForNonRetryableErrors() {
        BrandMessageConsumer consumer = new BrandMessageConsumer(kafkaReceiver, processBrandUseCase, objectMapper, rateLimiterRegistry);
        MessageProcessingException nonRetryableException = new MessageProcessingException("Error", new RuntimeException(), false);
        assertThat(invokeIsRetryableError(nonRetryableException)).isFalse();

        assertThat(invokeIsRetryableError(new RuntimeException("Generic error"))).isFalse();
    }

    @Test
    void cleanup_ShouldDisposeSubscription() {
        Disposable disposable = mock(Disposable.class);
        when(disposable.isDisposed()).thenReturn(false);
        setField(brandMessageConsumer, "subscription", disposable);

        brandMessageConsumer.cleanup();

        verify(disposable).dispose();
    }

    @Test
    void cleanup_ShouldHandleNullSubscription() {
        setField(brandMessageConsumer, "subscription", null);

        brandMessageConsumer.cleanup();
    }

    @Test
    void cleanup_ShouldHandleAlreadyDisposedSubscription() {
        Disposable disposable = mock(Disposable.class);
        when(disposable.isDisposed()).thenReturn(true);
        setField(brandMessageConsumer, "subscription", disposable);

        brandMessageConsumer.cleanup();

        verify(disposable, never()).dispose();
    }

    private boolean invokeIsRetryableError(Throwable error) {
        try {
            var method = BrandMessageConsumer.class.getDeclaredMethod("isRetryableError", Throwable.class);
            method.setAccessible(true);
            return (boolean) method.invoke(brandMessageConsumer, error);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static AbstractBooleanAssert<?> assertThat(boolean value) {
        return org.assertj.core.api.Assertions.assertThat(value);
    }
}