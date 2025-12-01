package com.fipe.processor.application.usecases;

import com.fipe.processor.application.ports.CacheServicePort;
import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.domain.repositories.BrandRepository;
import com.fipe.processor.domain.repositories.VehicleRepository;
import com.fipe.processor.presentation.exception.BrandNotFoundException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetVehiclesByBrandUseCaseTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CacheServicePort cacheService;

    @InjectMocks
    private GetVehiclesByBrandUseCase useCase;

    public GetVehiclesByBrandUseCaseTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testExecute_ReturnsVehiclesFromCache() {
        String brandName = "Toyota";
        String cacheKey = "vehicles:brand:toyota";

        List<Vehicle> vehicleList = new ArrayList<>();
        vehicleList.add(Vehicle.builder().id(1L).build());
        vehicleList.add(Vehicle.builder().id(2L).build());

        when(cacheService.get(eq(cacheKey), eq(Vehicle[].class)))
                .thenReturn(Mono.just(vehicleList.toArray(new Vehicle[0])));

        when(vehicleRepository.findByBrandName(anyString(), any(BrandRepository.class)))
                .thenReturn(Flux.empty());

        StepVerifier.create(useCase.execute(brandName))
                .expectNextSequence(vehicleList)
                .verifyComplete();

        verify(cacheService).get(eq(cacheKey), eq(Vehicle[].class));
    }

    @Test
    void testExecute_RetrievesFromDatabaseAndCachesResults_WhenCacheIsEmpty() {
        String brandName = "Honda";
        String cacheKey = "vehicles:brand:honda";

        List<Vehicle> vehicleList = new ArrayList<>();
        vehicleList.add(Vehicle.builder().id(3L).build());
        vehicleList.add(Vehicle.builder().id(4L).build());

        when(cacheService.get(eq(cacheKey), eq(Vehicle[].class)))
                .thenReturn(Mono.empty());
        when(vehicleRepository.findByBrandName(eq(brandName), eq(brandRepository)))
                .thenReturn(Flux.fromIterable(vehicleList));
        when(cacheService.put(eq(cacheKey), eq(vehicleList), eq(Duration.ofMinutes(30))))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(brandName))
                .expectNextSequence(vehicleList)
                .verifyComplete();

        verify(cacheService).get(eq(cacheKey), eq(Vehicle[].class));
        verify(vehicleRepository).findByBrandName(eq(brandName), eq(brandRepository));
        verify(cacheService).put(eq(cacheKey), eq(vehicleList), eq(Duration.ofMinutes(30)));
    }

    @Test
    void testExecute_ThrowsBrandNotFoundException_WhenNoResultsFromCacheOrDatabase() {
        String brandName = "NonExistentBrand";
        String cacheKey = "vehicles:brand:nonexistentbrand";

        when(cacheService.get(eq(cacheKey), eq(Vehicle[].class)))
                .thenReturn(Mono.empty());
        when(vehicleRepository.findByBrandName(eq(brandName), eq(brandRepository)))
                .thenReturn(Flux.empty());

        StepVerifier.create(useCase.execute(brandName))
                .expectError(BrandNotFoundException.class)
                .verify();

        verify(cacheService).get(eq(cacheKey), eq(Vehicle[].class));
        verify(vehicleRepository).findByBrandName(eq(brandName), eq(brandRepository));
    }

    @Test
    void testExecute_HandlesCacheErrorAndRetrievesFromDatabase() {
        String brandName = "Ford";
        String cacheKey = "vehicles:brand:ford";

        List<Vehicle> vehicleList = new ArrayList<>();
        vehicleList.add(Vehicle.builder().id(5L).build());

        when(cacheService.get(eq(cacheKey), eq(Vehicle[].class)))
                .thenReturn(Mono.error(new RuntimeException("Cache error")));
        when(vehicleRepository.findByBrandName(eq(brandName), eq(brandRepository)))
                .thenReturn(Flux.fromIterable(vehicleList));
        when(cacheService.put(eq(cacheKey), eq(vehicleList), eq(Duration.ofMinutes(30))))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(brandName))
                .expectNextSequence(vehicleList)
                .verifyComplete();

        verify(cacheService).get(eq(cacheKey), eq(Vehicle[].class));
        verify(vehicleRepository).findByBrandName(eq(brandName), eq(brandRepository));
        verify(cacheService).put(eq(cacheKey), eq(vehicleList), eq(Duration.ofMinutes(30)));
    }

    @Test
    void testExecute_HandlesDatabaseError() {
        String brandName = "Chevrolet";
        String cacheKey = "vehicles:brand:chevrolet";

        when(cacheService.get(eq(cacheKey), eq(Vehicle[].class)))
                .thenReturn(Mono.empty());
        when(vehicleRepository.findByBrandName(eq(brandName), eq(brandRepository)))
                .thenReturn(Flux.error(new RuntimeException("Database error")));

        StepVerifier.create(useCase.execute(brandName))
                .expectError(RuntimeException.class)
                .verify();

        verify(cacheService).get(eq(cacheKey), eq(Vehicle[].class));
    }
}