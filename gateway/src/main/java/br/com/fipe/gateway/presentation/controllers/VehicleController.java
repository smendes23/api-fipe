package br.com.fipe.gateway.presentation.controllers;

import br.com.fipe.gateway.application.ports.input.CommandServicePort;
import br.com.fipe.gateway.presentation.dto.request.UpdateVehicleRequest;
import br.com.fipe.gateway.presentation.dto.response.BrandResponse;
import br.com.fipe.gateway.presentation.dto.response.VehicleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "VEHICLES", description = "Vehicle management endpoints")
public class VehicleController {

    private final CommandServicePort service;


    @PostMapping("/vehicles/load")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Load FIPE data", description = "Initiates the process of loading vehicle data from FIPE API")
    public Mono<String> loadData() {
        log.info("Initiating FIPE data load");
        
        return service.dataLoad();
    }

    @GetMapping("/brands")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get all brands", description = "Retrieves all vehicle brands from the database")
    public Flux<BrandResponse> getBrands() {
        log.info("Retrieving all brands");
        
        return service.getBrands();
    }

    @GetMapping("/vehicles/{brandName}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get vehicles by brand", description = "Retrieves all vehicles for a specific brand")
    public Flux<VehicleResponse> getVehiclesByBrand(@PathVariable String brandName) {
        log.info("Retrieving vehicles for brand: {}", brandName);

        return service.getVehiclesByBrand(brandName);
    }

    @PutMapping("/vehicles/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update vehicle", description = "Updates vehicle model and observations")
    public Mono<VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        
        log.info("Updating vehicle: {}", id);
        
        return service.updateVehicle(id,request);
    }
}
