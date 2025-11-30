package br.com.fipe.gateway.application.ports;

import reactor.core.publisher.Mono;

public interface MessagePublisherPort {

    <T> Mono<Void> publish(String topic, String key, T message);
}
