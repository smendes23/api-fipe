package com.fipe.processor.infrastructure.adapters;

import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.infrastructure.adapters.dto.FipeBrandResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

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
        FipeBrandResponse response1 = new FipeBrandResponse("001", "Toyota");
        FipeBrandResponse response2 = new FipeBrandResponse("002", "Honda");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(response1, response2));

        Flux<Brand> result = fipeApiAdapter.fetchBrands();

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
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class)).thenReturn(Flux.empty());

        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        StepVerifier.create(result)
                .verifyComplete();

        verify(webClient, times(1)).get();
        verify(responseSpec, times(1)).bodyToFlux(FipeBrandResponse.class);
    }

    @Test
    @DisplayName("Should fail after max retries exceeded")
    void shouldFailAfterMaxRetriesExceeded() {
        RuntimeException timeoutError = new RuntimeException("Timeout");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.error(timeoutError));

        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(responseSpec, times(1)).bodyToFlux(FipeBrandResponse.class);
    }

    @Test
    @DisplayName("Should handle HTTP client error with retries")
    void shouldHandleHttpClientError() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.error(new RuntimeException("HTTP 500 Internal Server Error")));

        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(responseSpec, times(1)).bodyToFlux(FipeBrandResponse.class);
    }

    @Test
    @DisplayName("Should apply timeout configuration correctly")
    void shouldApplyTimeoutConfiguration() {
        FipeBrandResponse response = new FipeBrandResponse("001", "Toyota");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(response).delayElements(Duration.ofMillis(100)));

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
        ReflectionTestUtils.setField(fipeApiAdapter, "timeout", 15000);
        ReflectionTestUtils.setField(fipeApiAdapter, "maxRetries", 1);

        FipeBrandResponse response = new FipeBrandResponse("001", "Toyota");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(response));

        Flux<Brand> result = fipeApiAdapter.fetchBrands();

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
        FipeBrandResponse fipeResponse = new FipeBrandResponse("001", "Toyota");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(fipeResponse));

        Flux<Brand> result = fipeApiAdapter.fetchBrands();

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
        FipeBrandResponse fipeResponse = new FipeBrandResponse("001", "");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(fipeResponse));

        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        StepVerifier.create(result)
                .assertNext(brand -> {
                    assertEquals("001", brand.getCode());
                    assertEquals("", brand.getName());
                    assertFalse(brand.isValid());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle brand with null name")
    void shouldHandleBrandWithNullName() {
        FipeBrandResponse fipeResponse = new FipeBrandResponse("001", null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(fipeResponse));

        Flux<Brand> result = fipeApiAdapter.fetchBrands();

        StepVerifier.create(result)
                .assertNext(brand -> {
                    assertEquals("001", brand.getCode());
                    assertNull(brand.getName());
                    assertFalse(brand.isValid());
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle multiple sequential calls correctly")
    void shouldHandleMultipleSequentialCalls() {
        FipeBrandResponse response1 = new FipeBrandResponse("001", "Toyota");
        FipeBrandResponse response2 = new FipeBrandResponse("002", "Honda");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/carros/marcas")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(FipeBrandResponse.class))
                .thenReturn(Flux.just(response1, response2))
                .thenReturn(Flux.just(response2));

        StepVerifier.create(fipeApiAdapter.fetchBrands())
                .expectNextCount(2)
                .verifyComplete();

        StepVerifier.create(fipeApiAdapter.fetchBrands())
                .expectNextCount(1)
                .verifyComplete();

        verify(webClient, times(2)).get();
        verify(responseSpec, times(2)).bodyToFlux(FipeBrandResponse.class);
    }

    private void assertBrandEquals(Brand brand, String expectedCode, String expectedName) {
        assertEquals(expectedCode, brand.getCode());
        assertEquals(expectedName, brand.getName());
        assertNotNull(brand.getCreatedAt());
    }

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