package com.fipe.processor.application.usecases;

import com.fipe.processor.application.ports.UpdateVehicleServicePort;
import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.domain.repositories.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateVehicleUseCase implements UpdateVehicleServicePort {

    private final VehicleRepository vehicleRepository;

    public Mono<Vehicle> execute(Long vehicleId, String newModel, String newObservations) {
        log.debug("Updating vehicle: {}", vehicleId);
        
        return vehicleRepository.findById(vehicleId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Vehicle not found with id: " + vehicleId)))
                .flatMap(vehicle -> updateVehicleData(vehicle, newModel, newObservations))
                .flatMap(vehicleRepository::save)
                .doOnSuccess(updated -> log.info("Vehicle updated: {}", updated.getId()))
                .doOnError(error -> log.error("Error updating vehicle {}: {}", 
                        vehicleId, error.getMessage()));
    }

    private Mono<Vehicle> updateVehicleData(Vehicle vehicle, String newModel, String newObservations) {
        vehicle.update(newModel, newObservations);
        
        if (!vehicle.isValid()) {
            return Mono.error(new IllegalArgumentException("Invalid vehicle data"));
        }
        
        return Mono.just(vehicle);
    }
}
