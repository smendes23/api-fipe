package com.fipe.processor.application.usecases;

import com.fipe.processor.application.ports.output.FipeServicePort;
import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.domain.repositories.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessBrandUseCaseTest {

    @Mock
    private FipeServicePort fipeService;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private ProcessBrandUseCase processBrandUseCase;

    public ProcessBrandUseCaseTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldProcessAndSaveVehiclesForBrand() {
        Brand brandMessage = Brand.builder()
                .id(1L)
                .code("B123")
                .name("Brand123")
                .build();

        Vehicle vehicle1 = Vehicle.builder()
                .id(1L)
                .code("V001")
                .brandCode("B123")
                .model("Model1")
                .build();

        Vehicle vehicle2 = Vehicle.builder()
                .id(2L)
                .code("V002")
                .brandCode("B123")
                .model("Model2")
                .build();

        when(fipeService.fetchVehiclesByBrand("B123")).thenReturn(Flux.just(vehicle1, vehicle2));
        when(vehicleRepository.existsByCodeAndBrandCode("V001", "B123")).thenReturn(Mono.just(false));
        when(vehicleRepository.existsByCodeAndBrandCode("V002", "B123")).thenReturn(Mono.just(false));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(Mono.just(vehicle1), Mono.just(vehicle2));

        Flux<Vehicle> result = processBrandUseCase.execute(brandMessage);

        StepVerifier.create(result)
                .expectNext(vehicle1)
                .expectNext(vehicle2)
                .verifyComplete();

        verify(fipeService, times(1)).fetchVehiclesByBrand("B123");
        verify(vehicleRepository, times(1)).existsByCodeAndBrandCode("V001", "B123");
        verify(vehicleRepository, times(1)).existsByCodeAndBrandCode("V002", "B123");
        verify(vehicleRepository, times(2)).save(any(Vehicle.class));
    }

    @Test
    void shouldSkipSavingAlreadyExistingVehicles() {
        Brand brandMessage = Brand.builder()
                .id(1L)
                .code("B123")
                .name("Brand123")
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(1L)
                .code("V001")
                .brandCode("B123")
                .model("Model1")
                .build();

        when(fipeService.fetchVehiclesByBrand("B123")).thenReturn(Flux.just(vehicle));
        when(vehicleRepository.existsByCodeAndBrandCode("V001", "B123")).thenReturn(Mono.just(true));

        Flux<Vehicle> result = processBrandUseCase.execute(brandMessage);

        StepVerifier.create(result)
                .verifyComplete();

        verify(fipeService, times(1)).fetchVehiclesByBrand("B123");
        verify(vehicleRepository, times(1)).existsByCodeAndBrandCode("V001", "B123");
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void shouldHandleErrorDuringVehicleFetching() {
        Brand brandMessage = Brand.builder()
                .id(1L)
                .code("B123")
                .name("Brand123")
                .build();

        when(fipeService.fetchVehiclesByBrand("B123")).thenReturn(Flux.error(new RuntimeException("Service error")));

        Flux<Vehicle> result = processBrandUseCase.execute(brandMessage);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("Service error"))
                .verify();

        verify(fipeService, times(1)).fetchVehiclesByBrand("B123");
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void shouldHandleErrorDuringVehicleSaving() {
        Brand brandMessage = Brand.builder()
                .id(1L)
                .code("B123")
                .name("Brand123")
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(1L)
                .code("V001")
                .brandCode("B123")
                .model("Model1")
                .build();

        when(fipeService.fetchVehiclesByBrand("B123")).thenReturn(Flux.just(vehicle));
        when(vehicleRepository.existsByCodeAndBrandCode("V001", "B123")).thenReturn(Mono.just(false));
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        Flux<Vehicle> result = processBrandUseCase.execute(brandMessage);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().equals("Database error"))
                .verify();

        verify(fipeService, times(1)).fetchVehiclesByBrand("B123");
        verify(vehicleRepository, times(1)).existsByCodeAndBrandCode("V001", "B123");
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }
}