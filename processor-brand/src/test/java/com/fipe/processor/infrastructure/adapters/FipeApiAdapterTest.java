package com.fipe.processor.infrastructure.adapters;

import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.infrastructure.adapters.dto.FipeBrandResponse;
import com.fipe.processor.infrastructure.adapters.mappers.DomainMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FipeApiAdapterTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private FipeApiAdapter fipeApiAdapter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fipeApiAdapter, "timeout", 30000);
        ReflectionTestUtils.setField(fipeApiAdapter, "maxRetries", 2);
    }

    @Test
    @DisplayName("Should fetch brands successfully and map to domain")
    void shouldFetchBrandsSuccessfully() {
        // Arrange
        FipeBrandResponse response1 = new FipeBrandResponse("001", "Toyota");
        FipeBrandResponse response2 = new FipeBrandResponse("002", "Honda");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(response1, response2));

        // Act
        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        // Assert
        StepVerifier.create(result)
                .assertNext(brand -> {
                    assertBrandEquals(brand, "001", "Toyota");
                    assert brand.isValid();
                })
                .assertNext(brand -> {
                    assertBrandEquals(brand, "002", "Honda");
                    assert brand.isValid();
                })
                .verifyComplete();

        verify(webClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri("/carros/marcas");
        verify(requestHeadersSpec, times(1)).retrieve();
        verify(responseSpec, times(1)).bodyToFlux(FipeBrandResponse.class);
    }

    @Test
    @DisplayName("Should return empty flux when no brands found")
    void shouldReturnEmptyFluxWhenNoBrandsFound() {
        // Arrange
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class)).thenReturn(Flux.empty());

        // Act
        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(webClient, times(1)).get();
        verify(responseSpec, times(1)).bodyToFlux(FipeBrandResponse.class);
    }

    @Test
    @DisplayName("Should fail after max retries exceeded")
    void shouldFailAfterMaxRetriesExceeded() {
        // Arrange
        RuntimeException timeoutError = new RuntimeException("Timeout");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.error(timeoutError));

        // Act
        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        // Should retry maxRetries (2) times, so total calls = 1 initial + 2 retries = 3
        verify(responseSpec, times(1)).bodyToFlux(FipeBrandResponse.class);
    }

    @Test
    @DisplayName("Should handle HTTP client error with retries")
    void shouldHandleHttpClientError() {
        // Arrange
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.error(new RuntimeException("HTTP 500 Internal Server Error")));

        // Act
        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(responseSpec, times(1)).bodyToFlux(FipeBrandResponse.class); // 1 initial + 2 retries
    }

    @Test
    @DisplayName("Should apply timeout configuration correctly")
    void shouldApplyTimeoutConfiguration() {
        // Arrange
        FipeBrandResponse response = new FipeBrandResponse("001", "Toyota");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(response).delayElements(Duration.ofMillis(100)));

        // Act & Assert - Should complete within timeout (30 seconds)
        StepVerifier.create(fipeApiAdapter.fetchBrands())
                .assertNext(brand -> {
                    assertBrandEquals(brand, "001", "Toyota");
                    assert brand.isValid();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should use custom timeout and retry values from configuration")
    void shouldUseCustomTimeoutAndRetryValues() {
        // Arrange
        ReflectionTestUtils.setField(fipeApiAdapter, "timeout", 15000);
        ReflectionTestUtils.setField(fipeApiAdapter, "maxRetries", 1);

        FipeBrandResponse response = new FipeBrandResponse("001", "Toyota");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(response));

        // Act
        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        // Assert
        StepVerifier.create(result)
                .assertNext(brand -> {
                    assertBrandEquals(brand, "001", "Toyota");
                    assert brand.isValid();
                })
                .verifyComplete();

        verify(responseSpec, times(1)).bodyToFlux(FipeBrandResponse.class);
    }

    @Test
    @DisplayName("Should create valid Brand domain objects from FipeBrandResponse")
    void shouldCreateValidBrandDomainObjects() {
        // Arrange
        FipeBrandResponse fipeResponse = new FipeBrandResponse("001", "Toyota");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(fipeResponse));

        // Act
        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        // Assert
        StepVerifier.create(result)
                .assertNext(brand -> {
                    assertEquals("001", brand.getCode());
                    assertEquals("Toyota", brand.getName());
                    assertNotNull(brand.getCreatedAt());
                    assertTrue(brand.isValid());
                    assertTrue(brand.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
                    assertTrue(brand.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(1)));
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle brand with empty name but valid code")
    void shouldHandleBrandWithEmptyName() {
        // Arrange
        FipeBrandResponse fipeResponse = new FipeBrandResponse("001", "");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(fipeResponse));

        // Act
        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        // Assert
        StepVerifier.create(result)
                .assertNext(brand -> {
                    assertEquals("001", brand.getCode());
                    assertEquals("", brand.getName());
                    assertFalse(brand.isValid()); // Should be invalid because name is empty
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle brand with null name")
    void shouldHandleBrandWithNullName() {
        // Arrange
        FipeBrandResponse fipeResponse = new FipeBrandResponse("001", null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(fipeResponse));

        // Act
        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        // Assert
        StepVerifier.create(result)
                .assertNext(brand -> {
                    assertEquals("001", brand.getCode());
                    assertNull(brand.getName());
                    assertFalse(brand.isValid()); // Should be invalid because name is null
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle multiple sequential calls correctly")
    void shouldHandleMultipleSequentialCalls() {
        // Arrange
        FipeBrandResponse response1 = new FipeBrandResponse("001", "Toyota");
        FipeBrandResponse response2 = new FipeBrandResponse("002", "Honda");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(response1, response2))
                .thenReturn(Flux.just(response2));

        // Act & Assert - First call
        StepVerifier.create(fipeApiAdapter.fetchBrands())
                .expectNextCount(2)
                .verifyComplete();

        // Act & Assert - Second call
        StepVerifier.create(fipeApiAdapter.fetchBrands())
                .expectNextCount(1)
                .verifyComplete();

        verify(webClient, times(2)).get();
        verify(responseSpec, times(2)).bodyToFlux(FipeBrandResponse.class);
    }

    // Helper method for assertions
    private void assertBrandEquals(Brand brand, String expectedCode, String expectedName) {
        assertEquals(expectedCode, brand.getCode());
        assertEquals(expectedName, brand.getName());
        assertNotNull(brand.getCreatedAt());
    }

    // Helper methods to avoid static imports in the main class
    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private void assertNotNull(Object object) {
        org.junit.jupiter.api.Assertions.assertNotNull(object);
    }

    private void assertNull(Object object) {
        org.junit.jupiter.api.Assertions.assertNull(object);
    }

    private void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }

    private void assertFalse(boolean condition) {
        org.junit.jupiter.api.Assertions.assertFalse(condition);
    }
}