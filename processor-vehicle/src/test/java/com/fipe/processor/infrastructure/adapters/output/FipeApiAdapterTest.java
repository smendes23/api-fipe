package com.fipe.processor.infrastructure.adapters.output;

import com.fipe.processor.domain.dto.FipeModelsWrapper;
import com.fipe.processor.infrastructure.adapters.output.dto.FipeVehicleResponse;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

    private final String brandCode = "1";
    private final FipeVehicleResponse vehicleResponse1 =
            new FipeVehicleResponse("101", "Model 1");
    private final FipeVehicleResponse vehicleResponse2 =
            new FipeVehicleResponse("102", "Model 2");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fipeApiAdapter, "timeout", 30000);
        ReflectionTestUtils.setField(fipeApiAdapter, "maxRetries", 2);

        AtomicInteger requestCount = (AtomicInteger) ReflectionTestUtils.getField(fipeApiAdapter, "requestCount");
        AtomicLong lastResetTime = (AtomicLong) ReflectionTestUtils.getField(fipeApiAdapter, "lastResetTime");
        requestCount.set(0);
        lastResetTime.set(System.currentTimeMillis());
    }

    @Test
    void fetchVehiclesByBrand_ShouldReturnVehicles_WhenApiCallSucceeds() {
        FipeModelsWrapper wrapper =
                new FipeModelsWrapper(List.of(vehicleResponse1, vehicleResponse2));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(FipeModelsWrapper.class))
                .thenReturn(Mono.just(wrapper));

        StepVerifier.create(fipeApiAdapter.fetchVehiclesByBrand(brandCode))
                .expectNextMatches(vehicle ->
                        vehicle.getCode().equals("101") &&
                                vehicle.getModel().equals("Model 1") &&
                                vehicle.getBrandCode().equals(brandCode))
                .expectNextMatches(vehicle ->
                        vehicle.getCode().equals("102") &&
                                vehicle.getModel().equals("Model 2") &&
                                vehicle.getBrandCode().equals(brandCode))
                .verifyComplete();

        verify(webClient, times(1)).get();
    }

    @Test
    void fetchVehiclesByBrand_ShouldReturnEmpty_WhenNoModelsFound() {
        FipeModelsWrapper wrapper = new FipeModelsWrapper(List.of());

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(FipeModelsWrapper.class))
                .thenReturn(Mono.just(wrapper));

        StepVerifier.create(fipeApiAdapter.fetchVehiclesByBrand(brandCode))
                .verifyComplete();

        verify(webClient, times(1)).get();
    }

    @Test
    void fetchVehiclesByBrand_ShouldReturnEmpty_WhenModelsIsNull() {
        FipeModelsWrapper wrapper = new FipeModelsWrapper(null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(FipeModelsWrapper.class))
                .thenReturn(Mono.just(wrapper));

        StepVerifier.create(fipeApiAdapter.fetchVehiclesByBrand(brandCode))
                .verifyComplete();

        verify(webClient, times(1)).get();
    }


    @Test
    void fetchVehiclesByBrand_ShouldPropagateError_WhenNonRetryableErrorOccurs() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(FipeModelsWrapper.class))
                .thenReturn(Mono.error(WebClientResponseException.create(400, "Bad Request", null, null, null)));

        StepVerifier.create(fipeApiAdapter.fetchVehiclesByBrand(brandCode))
                .expectError(WebClientResponseException.class)
                .verify();

        verify(webClient, times(1)).get(); // No retries for non-retryable errors
    }


    @Test
    void fetchVehiclesByBrand_ShouldThrowException_WhenRateLimitExceeded() {
        AtomicInteger requestCount = (AtomicInteger) ReflectionTestUtils.getField(fipeApiAdapter, "requestCount");
        requestCount.set(451); // Exceed limit

        StepVerifier.create(fipeApiAdapter.fetchVehiclesByBrand(brandCode))
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Daily rate limit exceeded"))
                .verify();

        verify(webClient, never()).get();
    }

    @Test
    void shouldRetry_ShouldReturnTrue_For429Error() {
        WebClientResponseException exception = WebClientResponseException.create(429, "Too Many Requests", null, null, null);

        boolean result = fipeApiAdapter.shouldRetry(exception);

        assertThat(result).isTrue();
    }

    @Test
    void shouldRetry_ShouldReturnTrue_For5xxError() {
        WebClientResponseException exception = WebClientResponseException.create(500, "Server Error", null, null, null);

        boolean result = fipeApiAdapter.shouldRetry(exception);

        assertThat(result).isTrue();
    }

    @Test
    void shouldRetry_ShouldReturnFalse_For4xxError() {
        WebClientResponseException exception = WebClientResponseException.create(400, "Bad Request", null, null, null);

        boolean result = fipeApiAdapter.shouldRetry(exception);

        assertThat(result).isFalse();
    }

    @Test
    void shouldRetry_ShouldReturnTrue_ForTimeoutException() {
        TimeoutException exception = new TimeoutException("Request timeout");

        boolean result = fipeApiAdapter.shouldRetry(exception);

        assertThat(result).isTrue();
    }

    @Test
    void shouldRetry_ShouldReturnFalse_ForOtherExceptions() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        boolean result = fipeApiAdapter.shouldRetry(exception);

        assertThat(result).isFalse();
    }
}