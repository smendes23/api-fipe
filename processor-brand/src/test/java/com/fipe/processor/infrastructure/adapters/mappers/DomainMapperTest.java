package com.fipe.processor.infrastructure.adapters.mappers;

import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.infrastructure.adapters.dto.FipeBrandResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainMapperTest {



    @Test
    void shouldMapFipeBrandResponseToBrandWithValidData() {

        String expectedCode = "123";
        String expectedName = "Toyota";
        FipeBrandResponse mockResponse = new FipeBrandResponse(expectedCode, expectedName);

        Brand brand = DomainMapper.mapToDomain(mockResponse);

        assertEquals(expectedCode, brand.getCode());
        assertEquals(expectedName, brand.getName());
    }

    @Test
    void shouldHandleNullCodeAndNameGracefully() {
        FipeBrandResponse mockResponse = new FipeBrandResponse(null, null);

        Brand brand = DomainMapper.mapToDomain(mockResponse);

        assertEquals(null, brand.getCode());
        assertEquals(null, brand.getName());
    }

    @Test
    void shouldHandleEmptyCodeAndNameGracefully() {
        String expectedCode = "";
        String expectedName = "";
        FipeBrandResponse mockResponse = new FipeBrandResponse(expectedCode, expectedName);

        Brand brand = DomainMapper.mapToDomain(mockResponse);

        assertEquals(expectedCode, brand.getCode());
        assertEquals(expectedName, brand.getName());
    }
}