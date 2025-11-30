package com.fipe.processor.application.usecases;

import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.domain.repositories.VehicleRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateVehicleUseCaseTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private UpdateVehicleUseCase updateVehicleUseCase;

    @Test
    void execute_shouldUpdateVehicleSuccessfully() {
        
        Long vehicleId = 1L;
        String newModel = "New Model";
        String newObservations = "New observations";

        Vehicle existingVehicle = Vehicle.builder()
                .id(vehicleId)
                .code("V001")
                .brandCode("BRAND01")
                .model("Old Model")
                .observations("Old observations")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        Vehicle updatedVehicle = Vehicle.builder()
                .id(vehicleId)
                .code("V001")
                .brandCode("BRAND01")
                .model(newModel)
                .observations(newObservations)
                .createdAt(existingVehicle.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Mono.just(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(Mono.just(updatedVehicle));

        
        StepVerifier.create(updateVehicleUseCase.execute(vehicleId, newModel, newObservations))
                .expectNext(updatedVehicle)
                .verifyComplete();

        verify(vehicleRepository).findById(vehicleId);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void execute_shouldReturnErrorWhenVehicleNotFound() {
        
        Long vehicleId = 999L;
        String newModel = "New Model";
        String newObservations = "New observations";

        when(vehicleRepository.findById(vehicleId)).thenReturn(Mono.empty());

        
        StepVerifier.create(updateVehicleUseCase.execute(vehicleId, newModel, newObservations))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Vehicle not found with id: " + vehicleId))
                .verify();

        verify(vehicleRepository).findById(vehicleId);
        verifyNoMoreInteractions(vehicleRepository);
    }

    @Test
    void execute_shouldUpdateOnlyObservationsWhenModelIsNull() {
        
        Long vehicleId = 1L;
        String nullModel = null;
        String newObservations = "Updated observations";

        Vehicle existingVehicle = Vehicle.builder()
                .id(vehicleId)
                .code("V001")
                .brandCode("BRAND01")
                .model("Original Model")
                .observations("Old observations")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Mono.just(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle savedVehicle = invocation.getArgument(0);
            return Mono.just(savedVehicle);
        });

        
        StepVerifier.create(updateVehicleUseCase.execute(vehicleId, nullModel, newObservations))
                .expectNextMatches(vehicle ->
                        vehicle.getId().equals(vehicleId) &&
                                vehicle.getModel().equals("Original Model") && // Model não deve mudar
                                vehicle.getObservations().equals(newObservations) &&
                                vehicle.getUpdatedAt() != null
                )
                .verifyComplete();

        verify(vehicleRepository).findById(vehicleId);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void execute_shouldUpdateOnlyObservationsWhenModelIsBlank() {
        
        Long vehicleId = 1L;
        String blankModel = "   ";
        String newObservations = "Updated observations";

        Vehicle existingVehicle = Vehicle.builder()
                .id(vehicleId)
                .code("V001")
                .brandCode("BRAND01")
                .model("Original Model")
                .observations("Old observations")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(vehicleRepository.findById(vehicleId)).thenReturn(Mono.just(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle savedVehicle = invocation.getArgument(0);
            return Mono.just(savedVehicle);
        });

        
        StepVerifier.create(updateVehicleUseCase.execute(vehicleId, blankModel, newObservations))
                .expectNextMatches(vehicle ->
                        vehicle.getId().equals(vehicleId) &&
                                vehicle.getModel().equals("Original Model") && // Model não deve mudar
                                vehicle.getObservations().equals(newObservations) &&
                                vehicle.getUpdatedAt() != null
                )
                .verifyComplete();

        verify(vehicleRepository).findById(vehicleId);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void execute_shouldHandleRepositoryError() {
        
        Long vehicleId = 1L;
        String newModel = "New Model";
        String newObservations = "New observations";

        Vehicle existingVehicle = Vehicle.builder()
                .id(vehicleId)
                .code("V001")
                .brandCode("BRAND01")
                .model("Old Model")
                .observations("Old observations")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        RuntimeException repositoryError = new RuntimeException("Database error");

        when(vehicleRepository.findById(vehicleId)).thenReturn(Mono.just(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(Mono.error(repositoryError));

        
        StepVerifier.create(updateVehicleUseCase.execute(vehicleId, newModel, newObservations))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals("Database error"))
                .verify();

        verify(vehicleRepository).findById(vehicleId);
        verify(vehicleRepository).save(any(Vehicle.class));
    }
}