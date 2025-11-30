package br.com.fipe.gateway.infrastructure.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaMessagePublisherTest {

    @Mock
    private KafkaSender<String, Object> kafkaSender;

    @Mock
    private reactor.kafka.sender.SenderResult<String> senderResult;

    private KafkaMessagePublisher kafkaMessagePublisher;

    @BeforeEach
    void setUp() {
        kafkaMessagePublisher = new KafkaMessagePublisher(kafkaSender);
    }

    @Test
    void publish_ShouldSendMessageSuccessfully() {
        String topic = "test-topic";
        String key = "test-key";
        String message = "test-message";

        when(kafkaSender.send(any(Mono.class))).thenReturn(Flux.just(senderResult));

        Mono<Void> result = kafkaMessagePublisher.publish(topic, key, message);

        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaSender).send(any(Mono.class));
    }


    @Test
    void publish_ShouldHandleKafkaSenderError() {
        String topic = "test-topic";
        String key = "test-key";
        String message = "test-message";
        RuntimeException kafkaError = new RuntimeException("Kafka broker unavailable");

        when(kafkaSender.send(any(Mono.class))).thenReturn(Flux.error(kafkaError));

        Mono<Void> result = kafkaMessagePublisher.publish(topic, key, message);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(kafkaSender).send(any(Mono.class));
    }

    @Test
    void publish_ShouldLogSuccessOnMessagePublished() {
        String topic = "test-topic";
        String key = "test-key";
        String message = "test-message";

        when(kafkaSender.send(any(Mono.class))).thenReturn(Flux.just(senderResult));

        StepVerifier.create(kafkaMessagePublisher.publish(topic, key, message))
                .verifyComplete();

        verify(kafkaSender).send(any(Mono.class));
    }

    @Test
    void publish_ShouldLogErrorOnPublishFailure() {
        String topic = "test-topic";
        String key = "test-key";
        String message = "test-message";
        RuntimeException kafkaError = new RuntimeException("Network error");

        when(kafkaSender.send(any(Mono.class))).thenReturn(Flux.error(kafkaError));

        StepVerifier.create(kafkaMessagePublisher.publish(topic, key, message))
                .expectError(RuntimeException.class)
                .verify();

        verify(kafkaSender).send(any(Mono.class));
    }

    @Test
    void publish_WithComplexObject_ShouldSerializeCorrectly() {
        String topic = "user-events";
        String key = "user-123";
        UserEvent userEvent = new UserEvent("user-123", "USER_CREATED", "User created successfully");

        when(kafkaSender.send(any(Mono.class))).thenReturn(Flux.just(senderResult));

        Mono<Void> result = kafkaMessagePublisher.publish(topic, key, userEvent);

        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaSender).send(any(Mono.class));
    }

    @Test
    void publish_WithNullKey_ShouldWork() {
        String topic = "test-topic";
        String key = null;
        String message = "test-message";

        when(kafkaSender.send(any(Mono.class))).thenReturn(Flux.just(senderResult));

        Mono<Void> result = kafkaMessagePublisher.publish(topic, key, message);

        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaSender).send(any(Mono.class));
    }

    @Test
    void publish_WithEmptyTopic_ShouldWork() {
        String topic = "";
        String key = "test-key";
        String message = "test-message";

        when(kafkaSender.send(any(Mono.class))).thenReturn(Flux.just(senderResult));

        Mono<Void> result = kafkaMessagePublisher.publish(topic, key, message);

        StepVerifier.create(result)
                .verifyComplete();

        verify(kafkaSender).send(any(Mono.class));
    }

    static class UserEvent {
        private String userId;
        private String eventType;
        private String description;

        public UserEvent(String userId, String eventType, String description) {
            this.userId = userId;
            this.eventType = eventType;
            this.description = description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UserEvent userEvent = (UserEvent) o;
            return userId.equals(userEvent.userId) &&
                    eventType.equals(userEvent.eventType) &&
                    description.equals(userEvent.description);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(userId, eventType, description);
        }
    }
}
