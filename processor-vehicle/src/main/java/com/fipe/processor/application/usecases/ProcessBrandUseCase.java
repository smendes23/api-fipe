package com.fipe.processor.application.usecases;

import com.fipe.processor.application.ports.output.FipeServicePort;
import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.domain.repositories.VehicleRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.Objects.nonNull;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessBrandUseCase {

    private final FipeServicePort fipeService;
    private final VehicleRepository vehicleRepository;

    public Flux<Vehicle> execute(Brand brandMessage) {
        log.info("Processing brand: {} - {}", brandMessage.getCode(), brandMessage.getName());

        return fipeService.fetchVehiclesByBrand(brandMessage.getCode())
                .flatMap(this::saveVehicleIfNotExists)
                .doOnComplete(() -> log.info("Completed processing brand: {}", brandMessage.getCode()))
                .doOnError(error -> log.error("Error processing brand {}: {}", 
                        brandMessage.getCode(), error.getMessage()));
    }

    private Mono<Vehicle> saveVehicleIfNotExists(Vehicle vehicle) {
        return vehicleRepository.existsByCodeAndBrandCode(vehicle.getCode(), vehicle.getBrandCode())
                .flatMap(exists -> {
                    if (exists) {
                        log.debug("Vehicle already exists: {} - {}", vehicle.getCode(), vehicle.getModel());
                        return Mono.empty();
                    }
                    return saveVehicle(vehicle);
                });
    }

    private Mono<Vehicle> saveVehicle(Vehicle vehicle) {
        log.debug("Saving vehicle: {} - {} - {}", 
                vehicle.getBrandCode(), vehicle.getCode(), vehicle.getModel());
        
        return vehicleRepository.save(Vehicle.builder()
                        .brandCode(vehicle.getBrandCode())
                        .code(vehicle.getCode())
                        .model(vehicle.getModel())
                        .createdAt(nonNull(vehicle.getCreatedAt()) ?vehicle.getCreatedAt() : LocalDateTime.now() )
                        .build())
                .doOnSuccess(saved -> log.debug("Vehicle saved: {}", saved.getId()))
                .doOnError(error -> log.error("Error saving vehicle {}: {}", 
                        vehicle.getCode(), error.getMessage()));
    }
}
