package com.fipe.processor.application.ports;

import com.fipe.processor.domain.entities.Brand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GetBrandsServicePort {
    Flux<Brand> execute();
    Mono<Void> clearCache();
}
