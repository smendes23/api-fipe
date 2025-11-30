package com.fipe.processor.infrastructure.adapters.mappers;

import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.infrastructure.adapters.dto.FipeBrandResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainMapperTest {

    /**
     * Unit tests for the DomainMapper class.
     * The mapToDomain() method transforms a FipeBrandResponse object into a Brand object.
     */

    @Test
    void shouldMapFipeBrandResponseToBrandWithValidData() {
        // Arrange
        String expectedCode = "123";
        String expectedName = "Toyota";
        FipeBrandResponse mockResponse = new FipeBrandResponse(expectedCode, expectedName);

        // Act
        Brand brand = DomainMapper.mapToDomain(mockResponse);

        // Assert
        assertEquals(expectedCode, brand.getCode());
        assertEquals(expectedName, brand.getName());
    }

    @Test
    void shouldHandleNullCodeAndNameGracefully() {
        // Arrange
        FipeBrandResponse mockResponse = new FipeBrandResponse(null, null);

        // Act
        Brand brand = DomainMapper.mapToDomain(mockResponse);

        // Assert
        assertEquals(null, brand.getCode());
        assertEquals(null, brand.getName());
    }

    @Test
    void shouldHandleEmptyCodeAndNameGracefully() {
        // Arrange
        String expectedCode = "";
        String expectedName = "";
        FipeBrandResponse mockResponse = new FipeBrandResponse(expectedCode, expectedName);

        // Act
        Brand brand = DomainMapper.mapToDomain(mockResponse);

        // Assert
        assertEquals(expectedCode, brand.getCode());
        assertEquals(expectedName, brand.getName());
    }
}