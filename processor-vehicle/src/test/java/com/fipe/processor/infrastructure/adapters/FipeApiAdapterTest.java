package com.fipe.processor.infrastructure.adapters;

import com.fipe.processor.application.ports.output.FipeServicePort;
import com.fipe.processor.application.usecases.ProcessBrandUseCase;
import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.domain.repositories.VehicleRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessBrandUseCaseTest {

    @Mock
    private FipeServicePort fipeService;

    @Mock
    private VehicleRepository vehicleRepository;

    private ProcessBrandUseCase processBrandUseCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processBrandUseCase = new ProcessBrandUseCase(fipeService, vehicleRepository);
    }

    @Test
    void execute_ShouldProcessBrandAndSaveNewVehicles() {
        Brand brandMessage = new Brand(1L, "1", "Ford", LocalDateTime.now());
        Vehicle vehicle1 = Vehicle.create("fusion", "1", "Fusion"); // Usar "1" como brandCode
        Vehicle vehicle2 = Vehicle.create("mustang", "1", "Mustang"); // Usar "1" como brandCode

        when(fipeService.fetchVehiclesByBrand("1")) // Usar código "1" em vez de "ford"
                .thenReturn(Flux.just(vehicle1, vehicle2));
        when(vehicleRepository.existsByCodeAndBrandCode("fusion", "1"))
                .thenReturn(Mono.just(false));
        when(vehicleRepository.existsByCodeAndBrandCode("mustang", "1"))
                .thenReturn(Mono.just(false));
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(processBrandUseCase.execute(brandMessage))
                .expectNextCount(2)
                .verifyComplete();

        verify(fipeService).fetchVehiclesByBrand("1");
        verify(vehicleRepository, times(2)).existsByCodeAndBrandCode(anyString(), anyString());
        verify(vehicleRepository, times(2)).save(any(Vehicle.class));
    }

    @Test
    void execute_ShouldSkipExistingVehicles() {
        Brand brandMessage = new Brand(1L, "1", "Fiat", LocalDateTime.now());

        Vehicle existingVehicle = Vehicle.builder()
                .code("uno")
                .brandCode("1")
                .model("Uno")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Vehicle newVehicle = Vehicle.builder()
                .code("mobi")
                .brandCode("1")
                .model("Mobi")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(fipeService.fetchVehiclesByBrand("1"))
                .thenReturn(Flux.just(existingVehicle, newVehicle));
        when(vehicleRepository.existsByCodeAndBrandCode("uno", "1"))
                .thenReturn(Mono.just(true));
        when(vehicleRepository.existsByCodeAndBrandCode("mobi", "1"))
                .thenReturn(Mono.just(false));
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> {
                    Vehicle vehicleToSave = invocation.getArgument(0);
                    return Mono.just(vehicleToSave);
                });

        StepVerifier.create(processBrandUseCase.execute(brandMessage))
                .expectNextCount(1) // Apenas verifica que 1 veículo foi emitido
                .verifyComplete();

        verify(fipeService).fetchVehiclesByBrand("1");
        verify(vehicleRepository, times(2)).existsByCodeAndBrandCode(anyString(), anyString());
        verify(vehicleRepository, times(1)).save(any(Vehicle.class));
    }

    @Test
    void execute_ShouldHandleEmptyVehicleList() {
        Brand brandMessage = new Brand(2L, "2", "Chevrolet", LocalDateTime.now());

        when(fipeService.fetchVehiclesByBrand("2")) // Usar código "2"
                .thenReturn(Flux.empty());

        StepVerifier.create(processBrandUseCase.execute(brandMessage))
                .verifyComplete();

        verify(fipeService).fetchVehiclesByBrand("2");
        verify(vehicleRepository, never()).existsByCodeAndBrandCode(anyString(), anyString());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void execute_ShouldHandleErrorFromFipeService() {
        Brand brandMessage = new Brand(3L, "3", "Honda", LocalDateTime.now());
        String errorMessage = "API timeout";

        when(fipeService.fetchVehiclesByBrand("3")) // Usar código "3"
                .thenReturn(Flux.error(new RuntimeException(errorMessage)));

        StepVerifier.create(processBrandUseCase.execute(brandMessage))
                .verifyError(RuntimeException.class);

        verify(fipeService).fetchVehiclesByBrand("3");
        verify(vehicleRepository, never()).existsByCodeAndBrandCode(anyString(), anyString());
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void execute_ShouldHandleErrorDuringSave() {
        Brand brandMessage = new Brand(4L, "4", "Toyota", LocalDateTime.now());
        Vehicle vehicle = Vehicle.create("corolla", "4", "Corolla");

        when(fipeService.fetchVehiclesByBrand("4")) // Usar código "4"
                .thenReturn(Flux.just(vehicle));
        when(vehicleRepository.existsByCodeAndBrandCode("corolla", "4"))
                .thenReturn(Mono.just(false));
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        StepVerifier.create(processBrandUseCase.execute(brandMessage))
                .verifyError(RuntimeException.class);

        verify(fipeService).fetchVehiclesByBrand("4");
        verify(vehicleRepository).existsByCodeAndBrandCode("corolla", "4");
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void execute_ShouldHandlePartialErrorsAndContinueProcessing() {
        Brand brandMessage = new Brand(1L, "1", "Volkswagen", LocalDateTime.now());
        Vehicle vehicle1 = Vehicle.create("gol", "1", "Gol");
        Vehicle vehicle2 = Vehicle.create("polo", "1", "Polo");
        Vehicle vehicle3 = Vehicle.create("virtus", "1", "Virtus");

        when(fipeService.fetchVehiclesByBrand("1")) // Usar código "1"
                .thenReturn(Flux.just(vehicle1, vehicle2, vehicle3));
        when(vehicleRepository.existsByCodeAndBrandCode("gol", "1"))
                .thenReturn(Mono.just(false));
        when(vehicleRepository.existsByCodeAndBrandCode("polo", "1"))
                .thenReturn(Mono.just(true));
        when(vehicleRepository.existsByCodeAndBrandCode("virtus", "1"))
                .thenReturn(Mono.just(false));
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(processBrandUseCase.execute(brandMessage))
                .expectNextCount(2)
                .verifyComplete();

        verify(fipeService).fetchVehiclesByBrand("1");
        verify(vehicleRepository, times(3)).existsByCodeAndBrandCode(anyString(), anyString());
        verify(vehicleRepository, times(2)).save(any(Vehicle.class));
    }

    @Test
    void execute_ShouldPropagateCompleteSignal() {
        Brand brandMessage = new Brand(1L, "1", "Renault", LocalDateTime.now());
        Vehicle vehicle1 = Vehicle.create("clio", "1", "Clio");
        Vehicle vehicle2 = Vehicle.create("sandero", "1", "Sandero");

        when(fipeService.fetchVehiclesByBrand("1")) // Usar código "1"
                .thenReturn(Flux.just(vehicle1, vehicle2));
        when(vehicleRepository.existsByCodeAndBrandCode(anyString(), anyString()))
                .thenReturn(Mono.just(false));
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(processBrandUseCase.execute(brandMessage))
                .expectNextCount(2)
                .expectComplete()
                .verify();

        verify(fipeService).fetchVehiclesByBrand("1");
        verify(vehicleRepository, times(2)).existsByCodeAndBrandCode(anyString(), anyString());
        verify(vehicleRepository, times(2)).save(any(Vehicle.class));
    }

    @Test
    void execute_ShouldHandleVehicleWithNullCreatedAt() {
        Brand brandMessage = new Brand(1L, "1", "Hyundai", LocalDateTime.now());
        Vehicle vehicle = Vehicle.builder()
                .code("hb20")
                .brandCode("1") // Usar código "1"
                .model("HB20")
                .createdAt(null)
                .build();

        when(fipeService.fetchVehiclesByBrand("1")) // Usar código "1"
                .thenReturn(Flux.just(vehicle));
        when(vehicleRepository.existsByCodeAndBrandCode("hb20", "1"))
                .thenReturn(Mono.just(false));
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> {
                    Vehicle savedVehicle = invocation.getArgument(0);
                    if (savedVehicle.getCreatedAt() == null) {
                        return Mono.error(new RuntimeException("createdAt should not be null"));
                    }
                    return Mono.just(savedVehicle);
                });

        StepVerifier.create(processBrandUseCase.execute(brandMessage))
                .expectNextCount(1)
                .verifyComplete();

        verify(fipeService).fetchVehiclesByBrand("1");
        verify(vehicleRepository).existsByCodeAndBrandCode("hb20", "1");
        verify(vehicleRepository).save(any(Vehicle.class));
    }
}