package com.fipe.processor.presentation.mappers;

import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.presentation.dto.VehicleResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class EntityMapperTest {

    @InjectMocks
    private EntityMapper entityMapper;

    @Mock
    private Vehicle mockVehicle;

    public EntityMapperTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void toVehicleResponse_shouldMapVehicleToVehicleResponseSuccessfully() {
        when(mockVehicle.getId()).thenReturn(1L);
        when(mockVehicle.getCode()).thenReturn("X123");
        when(mockVehicle.getBrandCode()).thenReturn("BR456");
        when(mockVehicle.getModel()).thenReturn("Test Model");
        when(mockVehicle.getObservations()).thenReturn("Test Observations");

        VehicleResponse response = entityMapper.toVehicleResponse(mockVehicle);

        assertNotNull(response);
        assertEquals(mockVehicle.getId(), response.id());
        assertEquals(mockVehicle.getCode(), response.code());
        assertEquals(mockVehicle.getBrandCode(), response.brandCode());
        assertEquals(mockVehicle.getModel(), response.model());
        assertEquals(mockVehicle.getObservations(), response.observations());
    }

    @Test
    void toVehicleResponse_shouldHandleNullObservations() {
        when(mockVehicle.getId()).thenReturn(2L);
        when(mockVehicle.getCode()).thenReturn("Y789");
        when(mockVehicle.getBrandCode()).thenReturn("BR123");
        when(mockVehicle.getModel()).thenReturn("Another Model");
        when(mockVehicle.getObservations()).thenReturn(null);

        VehicleResponse response = entityMapper.toVehicleResponse(mockVehicle);

        assertNotNull(response);
        assertEquals(mockVehicle.getId(), response.id());
        assertEquals(mockVehicle.getCode(), response.code());
        assertEquals(mockVehicle.getBrandCode(), response.brandCode());
        assertEquals(mockVehicle.getModel(), response.model());
        assertNull(response.observations());
    }

    @Test
    void toVehicleResponse_shouldHandleMissingModel() {
        when(mockVehicle.getId()).thenReturn(3L);
        when(mockVehicle.getCode()).thenReturn("Z456");
        when(mockVehicle.getBrandCode()).thenReturn("BR987");
        when(mockVehicle.getModel()).thenReturn(null);
        when(mockVehicle.getObservations()).thenReturn("Some Observations");

        VehicleResponse response = entityMapper.toVehicleResponse(mockVehicle);

        assertNotNull(response);
        assertEquals(mockVehicle.getId(), response.id());
        assertEquals(mockVehicle.getCode(), response.code());
        assertEquals(mockVehicle.getBrandCode(), response.brandCode());
        assertNull(response.model());
        assertEquals(mockVehicle.getObservations(), response.observations());
    }
}