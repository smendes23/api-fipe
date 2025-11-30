package com.fipe.processor.application.ports;

import reactor.core.publisher.Mono;

public interface BrandMessagePublisherPort {

    <T> Mono<Void> publish(String topic, String key, T message);
}
