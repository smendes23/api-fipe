package com.fipe.processor.application.ports;

import com.fipe.processor.domain.entities.Vehicle;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GetVehiclesByBrandServicePort {
    Flux<Vehicle> execute(String brandCode);
    Mono<Void> clearCacheByBrand(String brandName);
}
