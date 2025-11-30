package com.fipe.processor.application.ports;

import com.fipe.processor.domain.entities.Vehicle;
import reactor.core.publisher.Mono;

public interface UpdateVehicleServicePort {
    Mono<Vehicle> execute(Long vehicleId, String newModel, String newObservations);
}
