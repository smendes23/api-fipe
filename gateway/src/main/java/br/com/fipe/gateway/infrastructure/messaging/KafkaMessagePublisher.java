package br.com.fipe.gateway.infrastructure.messaging;

import br.com.fipe.gateway.application.ports.output.MessagePublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessagePublisher implements MessagePublisherPort {

    private final KafkaSender<String, Object> kafkaSender;

    @Override
    public <T> Mono<Void> publish(String topic, String key, T message) {
        log.debug("Publishing message to topic: {} with key: {}", topic, key);

        ProducerRecord<String, Object> producerRecord = 
                new ProducerRecord<>(topic, key, message);

        SenderRecord<String, Object, String> senderRecord = 
                SenderRecord.create(producerRecord, key);

        return kafkaSender.send(Mono.just(senderRecord))
                .next()
                .doOnSuccess(result -> log.debug("Message published successfully to topic: {} with key: {}", 
                        topic, key))
                .doOnError(error -> log.error("Error publishing message to topic: {} with key: {}: {}", 
                        topic, key, error.getMessage()))
                .then();
    }
}
