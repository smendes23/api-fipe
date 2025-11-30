package com.fipe.processor.presentation.controllers;

import com.fipe.processor.application.ports.GetBrandsServicePort;
import com.fipe.processor.application.ports.GetVehiclesByBrandServicePort;
import com.fipe.processor.application.ports.LoadFipeDataPort;
import com.fipe.processor.application.ports.UpdateVehicleServicePort;
import com.fipe.processor.presentation.dto.BrandResponse;
import com.fipe.processor.presentation.dto.UpdateVehicleRequest;
import com.fipe.processor.presentation.dto.VehicleResponse;
import com.fipe.processor.presentation.mappers.EntityMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class VehicleController {

    private final LoadFipeDataPort loadFipeDataUseCase;
    private final GetBrandsServicePort getBrandsUseCase;
    private final GetVehiclesByBrandServicePort getVehiclesByBrandUseCase;
    private final UpdateVehicleServicePort updateVehicleUseCase;
    private final EntityMapper entityMapper;

    @Value("${cache.ttl.brands}")
    private long brandsCacheTtl;

    @Value("${cache.ttl.vehicles}")
    private long vehiclesCacheTtl;

    @PostMapping("/vehicles/load")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<String> loadData() {
        log.info("Initiating FIPE data load");
        
        return loadFipeDataUseCase.execute()
                .collectList()
                .map(brands -> "Data loading initiated. Processed " + brands.size() + " brands.")
                .doOnSuccess(result -> log.info("Data load completed: {}", result))
                .doOnError(error -> log.error("Error loading data: {}", error.getMessage()));
    }

    @GetMapping("/brands")
    @ResponseStatus(HttpStatus.OK)
    public Flux<BrandResponse> getBrands() {
        log.info("Retrieving all brands");
        
        return getBrandsUseCase.execute()
                .map(entityMapper::toBrandResponse)
                .collectList()
                .flatMapMany(Flux::fromIterable)
                .doOnComplete(() -> log.info("Brands retrieval completed"))
                .doOnError(error -> log.error("Error retrieving brands: {}", error.getMessage()));
    }

    @GetMapping("/vehicles")
    @ResponseStatus(HttpStatus.OK)
    public Flux<VehicleResponse> getVehiclesByBrand(@RequestParam String brandName) {
        log.info("Retrieving vehicles for brand: {}", brandName);
        
        return getVehiclesByBrandUseCase.execute(brandName)
                .map(entityMapper::toVehicleResponse)
                .collectList()
                .flatMapMany(Flux::fromIterable)
                .doOnComplete(() -> log.info("Vehicles retrieval completed for brand: {}", brandName))
                .doOnError(error -> log.error("Error retrieving vehicles for brand {}: {}",
                        brandName, error.getMessage()));
    }

    @PutMapping("/vehicles/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        
        log.info("Updating vehicle: {}", id);
        
        return updateVehicleUseCase.execute(id, request.model(), request.observations())
                .map(entityMapper::toVehicleResponse)
                .doOnSuccess(response -> log.info("Vehicle updated successfully: {}", id))
                .doOnError(error -> log.error("Error updating vehicle {}: {}", id, error.getMessage()));
    }
}
