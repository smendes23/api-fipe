package com.fipe.processor.infrastructure.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandMessagePublisherTest {

    @Mock
    private KafkaSender<String, Object> kafkaSender;

    @InjectMocks
    private BrandMessagePublisher brandMessagePublisher;

    @Test
    void shouldPublishMessageSuccessfully() {
        
        String topic = "brand-topic";
        String key = "brand-123";
        String message = "Test message";

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(kafkaSender.send(any(Mono.class)))
                .thenReturn(Flux.just(senderResult));

        
        Mono<Void> result = brandMessagePublisher.publish(topic, key, message);

        
        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    void shouldCreateCorrectRecords() {
        
        String topic = "brand-topic";
        String key = "brand-123";
        BrandMessage message = new BrandMessage("001", "Brand A");

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(kafkaSender.send(any(Mono.class)))
                .thenAnswer(invocation -> {
                    Mono<SenderRecord<String, Object, String>> senderRecordMono = invocation.getArgument(0);

                    // Verifica se o SenderRecord foi criado corretamente
                    StepVerifier.create(senderRecordMono)
                            .assertNext(senderRecord -> {
                                ProducerRecord<String, Object> producerRecord = senderRecord;
                                assert producerRecord.topic().equals(topic);
                                assert producerRecord.key().equals(key);
                                assert producerRecord.value().equals(message);
                                assert senderRecord.correlationMetadata().equals(key);
                            })
                            .verifyComplete();

                    return Flux.just(senderResult);
                });

        
        Mono<Void> result = brandMessagePublisher.publish(topic, key, message);

        
        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    @DisplayName("Should handle Kafka send error")
    void shouldHandleKafkaSendError() {
        
        String topic = "brand-topic";
        String key = "brand-123";
        String message = "Test message";
        RuntimeException kafkaError = new RuntimeException("Kafka connection failed");

        when(kafkaSender.send(any(Mono.class)))
                .thenReturn(Flux.error(kafkaError));

        StepVerifier.create(brandMessagePublisher.publish(topic, key, message))
                .expectError(RuntimeException.class)
                .verify();

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }
    
    @Test
    void shouldPublishMessageWithComplexObject() {
        
        String topic = "brand-events";
        String key = "event-456";
        BrandEvent event = new BrandEvent("CREATE", "001", "Brand A");

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(kafkaSender.send(any(Mono.class)))
                .thenReturn(Flux.just(senderResult));

        
        Mono<Void> result = brandMessagePublisher.publish(topic, key, event);

        
        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    void shouldPublishMessageWithNullKey() {
        
        String topic = "brand-topic";
        String key = null;
        String message = "Test message without key";

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(kafkaSender.send(any(Mono.class)))
                .thenReturn(Flux.just(senderResult));

        
        Mono<Void> result = brandMessagePublisher.publish(topic, key, message);

        
        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    void shouldUseSameKeyForCorrelationMetadata() {
        
        String topic = "brand-topic";
        String key = "correlation-test";
        String message = "Test correlation";

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(kafkaSender.send(any(Mono.class)))
                .thenAnswer(invocation -> {
                    Mono<SenderRecord<String, Object, String>> senderRecordMono = invocation.getArgument(0);

                    StepVerifier.create(senderRecordMono)
                            .assertNext(senderRecord -> {
                                // SenderRecord implementa ProducerRecord, então podemos acessar diretamente
                                assert senderRecord.topic().equals(topic);
                                assert senderRecord.key().equals(key);
                                assert senderRecord.value().equals(message);
                                assert senderRecord.correlationMetadata().equals(key);
                            })
                            .verifyComplete();

                    return Flux.just(senderResult);
                });

        
        Mono<Void> result = brandMessagePublisher.publish(topic, key, message);

        
        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }

    @Test
    void shouldVerifyCompleteFlow() {
        
        String topic = "brand-topic";
        String key = "test-key";
        String message = "test-message";

        @SuppressWarnings("unchecked")
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(kafkaSender.send(any(Mono.class)))
                .thenAnswer(invocation -> {
                    Mono<SenderRecord<String, Object, String>> senderRecordMono = invocation.getArgument(0);

                    // Captura e valida o SenderRecord
                    SenderRecord<String, Object, String> capturedRecord = senderRecordMono.block();
                    assert capturedRecord != null;
                    assert capturedRecord.topic().equals(topic);
                    assert capturedRecord.key().equals(key);
                    assert capturedRecord.value().equals(message);
                    assert capturedRecord.correlationMetadata().equals(key);

                    return Flux.just(senderResult);
                });

        
        Mono<Void> result = brandMessagePublisher.publish(topic, key, message);

        
        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaSender, times(1)).send(any(Mono.class));
    }
}

class BrandMessage {
    private String id;
    private String name;

    public BrandMessage(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrandMessage that = (BrandMessage) o;
        return java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name);
    }
}

class BrandEvent {
    private String type;
    private String brandId;
    private String brandName;

    public BrandEvent(String type, String brandId, String brandName) {
        this.type = type;
        this.brandId = brandId;
        this.brandName = brandName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrandEvent that = (BrandEvent) o;
        return java.util.Objects.equals(type, that.type) &&
                java.util.Objects.equals(brandId, that.brandId) &&
                java.util.Objects.equals(brandName, that.brandName);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, brandId, brandName);
    }
}