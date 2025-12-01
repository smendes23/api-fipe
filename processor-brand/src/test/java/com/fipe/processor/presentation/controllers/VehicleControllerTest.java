package com.fipe.processor.presentation.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipe.processor.application.ports.GetBrandsServicePort;
import com.fipe.processor.application.ports.GetVehiclesByBrandServicePort;
import com.fipe.processor.application.ports.LoadFipeDataPort;
import com.fipe.processor.application.ports.UpdateVehicleServicePort;
import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.presentation.dto.BrandResponse;
import com.fipe.processor.presentation.dto.UpdateVehicleRequest;
import com.fipe.processor.presentation.dto.VehicleResponse;
import com.fipe.processor.presentation.mappers.EntityMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    @Mock
    private LoadFipeDataPort loadFipeDataUseCase;

    @Mock
    private GetBrandsServicePort getBrandsUseCase;

    @Mock
    private GetVehiclesByBrandServicePort getVehiclesByBrandUseCase;

    @Mock
    private UpdateVehicleServicePort updateVehicleUseCase;

    @Mock
    private EntityMapper entityMapper;

    private VehicleController controller;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        controller = new VehicleController(
                loadFipeDataUseCase,
                getBrandsUseCase,
                getVehiclesByBrandUseCase,
                updateVehicleUseCase,
                entityMapper
        );

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void loadData_ShouldHandleError() {
        
        when(loadFipeDataUseCase.execute()).thenReturn(Flux.error(new RuntimeException("Service error")));

        
        Mono<String> result = controller.loadData()
                .map(brandsList -> "Data loading initiated. Processed " + brandsList.length() + " brands.");

        
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(loadFipeDataUseCase).execute();
    }

    @Test
    void getBrands_ShouldReturnBrandsList() {
        
        Brand testBrand = new Brand(1L, "1", "Acura", LocalDateTime.now());
        BrandResponse testBrandResponse = new BrandResponse("1", "Acura");

        List<Brand> brands = List.of(testBrand);
        when(getBrandsUseCase.execute()).thenReturn(Flux.fromIterable(brands));
        when(entityMapper.toBrandResponse(testBrand)).thenReturn(testBrandResponse);

        
        Flux<BrandResponse> result = controller.getBrands();

        
        StepVerifier.create(result)
                .expectNext(testBrandResponse)
                .verifyComplete();

        verify(getBrandsUseCase).execute();
        verify(entityMapper).toBrandResponse(testBrand);
    }

    @Test
    void getBrands_ShouldHandleEmptyList() {
        
        when(getBrandsUseCase.execute()).thenReturn(Flux.empty());

        
        Flux<BrandResponse> result = controller.getBrands();

        
        StepVerifier.create(result)
                .verifyComplete();

        verify(getBrandsUseCase).execute();
    }

    @Test
    void getBrands_ShouldHandleServiceError() {
        
        when(getBrandsUseCase.execute()).thenReturn(Flux.error(new RuntimeException("Database error")));

        
        Flux<BrandResponse> result = controller.getBrands();

        
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(getBrandsUseCase).execute();
    }

    @Test
    void getVehiclesByBrand_ShouldReturnVehicles() {
        
        String brandName = "Acura";
        Vehicle testVehicle = new Vehicle(1L, "1", "1", "Integra GS 1.8", null, LocalDateTime.now(), null);
        VehicleResponse testVehicleResponse = new VehicleResponse(1L, "001004-1", "1", "Integra GS 1.8", "Imported vehicle");

        List<Vehicle> vehicles = List.of(testVehicle);
        when(getVehiclesByBrandUseCase.execute(brandName)).thenReturn(Flux.fromIterable(vehicles));
        when(entityMapper.toVehicleResponse(testVehicle)).thenReturn(testVehicleResponse);

        
        Flux<VehicleResponse> result = controller.getVehiclesByBrand(brandName);

        
        StepVerifier.create(result)
                .expectNext(testVehicleResponse)
                .verifyComplete();

        verify(getVehiclesByBrandUseCase).execute(brandName);
        verify(entityMapper).toVehicleResponse(testVehicle);
    }

    @Test
    void getVehiclesByBrand_ShouldHandleEmptyList() {
        
        String brandName = "UnknownBrand";
        when(getVehiclesByBrandUseCase.execute(brandName)).thenReturn(Flux.empty());

        
        Flux<VehicleResponse> result = controller.getVehiclesByBrand(brandName);

        
        StepVerifier.create(result)
                .verifyComplete();

        verify(getVehiclesByBrandUseCase).execute(brandName);
    }

    @Test
    void getVehiclesByBrand_ShouldHandleServiceError() {
        
        String brandName = "Acura";
        when(getVehiclesByBrandUseCase.execute(brandName)).thenReturn(Flux.error(new RuntimeException("Brand not found")));

        
        Flux<VehicleResponse> result = controller.getVehiclesByBrand(brandName);

        
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(getVehiclesByBrandUseCase).execute(brandName);
    }

    @Test
    void updateVehicle_ShouldReturnUpdatedVehicle() {
        
        Long vehicleId = 1L;
        UpdateVehicleRequest request = new UpdateVehicleRequest("Integra GS 1.8 Special Edition", "Imported with custom modifications");
        Vehicle updatedVehicle = new Vehicle(vehicleId, "001004-1", "1", "Integra GS 1.8 Special Edition", "Imported with custom modifications", LocalDateTime.now(), LocalDateTime.now());
        VehicleResponse updatedResponse = new VehicleResponse(vehicleId, "001004-1", "1", "Integra GS 1.8 Special Edition", "Imported with custom modifications");

        when(updateVehicleUseCase.execute(vehicleId, request.model(), request.observations()))
                .thenReturn(Mono.just(updatedVehicle));
        when(entityMapper.toVehicleResponse(updatedVehicle)).thenReturn(updatedResponse);

        
        Mono<VehicleResponse> result = controller.updateVehicle(vehicleId, request);

        
        StepVerifier.create(result)
                .expectNext(updatedResponse)
                .verifyComplete();

        verify(updateVehicleUseCase).execute(vehicleId, request.model(), request.observations());
        verify(entityMapper).toVehicleResponse(updatedVehicle);
    }

    @Test
    void updateVehicle_ShouldHandleNotFound() {
        
        Long vehicleId = 999L;
        UpdateVehicleRequest request = new UpdateVehicleRequest("New Model", "New Observations");
        when(updateVehicleUseCase.execute(vehicleId, request.model(), request.observations()))
                .thenReturn(Mono.error(new IllegalArgumentException("Vehicle not found")));

        
        Mono<VehicleResponse> result = controller.updateVehicle(vehicleId, request);

        
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(updateVehicleUseCase).execute(vehicleId, request.model(), request.observations());
    }

    @Test
    void updateVehicle_ShouldHandleInvalidData() {
        
        Long vehicleId = 1L;
        UpdateVehicleRequest request = new UpdateVehicleRequest("", "Observations");
        when(updateVehicleUseCase.execute(vehicleId, request.model(), request.observations()))
                .thenReturn(Mono.error(new IllegalArgumentException("Invalid vehicle data")));

        
        Mono<VehicleResponse> result = controller.updateVehicle(vehicleId, request);

        
        StepVerifier.create(result)
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(updateVehicleUseCase).execute(vehicleId, request.model(), request.observations());
    }

    @Test
    void updateVehicle_ShouldHandleServiceError() {
        
        Long vehicleId = 1L;
        UpdateVehicleRequest request = new UpdateVehicleRequest("Model", "Observations");
        when(updateVehicleUseCase.execute(vehicleId, request.model(), request.observations()))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        
        Mono<VehicleResponse> result = controller.updateVehicle(vehicleId, request);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(updateVehicleUseCase).execute(vehicleId, request.model(), request.observations());
    }
}