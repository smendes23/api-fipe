package br.com.fipe.gateway.application.usecases;

import br.com.fipe.gateway.presentation.dto.response.BrandResponse;
import br.com.fipe.gateway.presentation.dto.response.VehicleResponse;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandServiceUseCaseTest {

    @Mock
    private WebClient webClient;

    @Mock
    private RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RequestBodySpec requestBodySpec;

    @Mock
    private ResponseSpec responseSpec;

    @Mock
    private ClientResponse clientResponse;

    private CommandServiceUseCase commandServiceUseCase;

    @BeforeEach
    void setUp() {
        commandServiceUseCase = new CommandServiceUseCase(webClient);
    }

    @Test
    void dataLoad_ShouldReturnSuccess() {
        String expectedResponse = "Data load completed successfully";

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/v1/vehicles/load")).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(expectedResponse));

        Mono<String> result = commandServiceUseCase.dataLoad();

        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/api/v1/vehicles/load");
        verify(requestBodySpec).retrieve();
        verify(responseSpec).bodyToMono(String.class);
    }

    @Test
    void dataLoad_ShouldHandleError() {
        RuntimeException expectedError = new RuntimeException("Network error");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/v1/vehicles/load")).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(expectedError));

        Mono<String> result = commandServiceUseCase.dataLoad();

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/api/v1/vehicles/load");
    }

    @Test
    void getBrands_ShouldReturnBrandsList() {
        List<BrandResponse> expectedBrands = List.of(
                new BrandResponse("1", "Acura"),
                new BrandResponse("2", "Audi")
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/v1/brands")).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(BrandResponse.class)).thenReturn(Flux.fromIterable(expectedBrands));

        Flux<BrandResponse> result = commandServiceUseCase.getBrands();

        StepVerifier.create(result)
                .expectNextSequence(expectedBrands)
                .verifyComplete();

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("/api/v1/brands");
        verify(requestBodySpec).accept(any());
    }

    @Test
    void getBrands_ShouldHandleClientError() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/v1/brands")).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(BrandResponse.class)).thenReturn(Flux.error(new RuntimeException("Client error")));

        Flux<BrandResponse> result = commandServiceUseCase.getBrands();

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri("/api/v1/brands");
    }

    @Test
    void getVehiclesByBrand_ShouldReturnVehicles() {
        String brandName = "Acura";
        List<VehicleResponse> expectedVehicles = List.of(
                new VehicleResponse(1L, "001004-1", "1", "Integra GS 1.8", "Imported vehicle"),
                new VehicleResponse(2L, "001005-1", "1", "NSX", "Sports car")
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(VehicleResponse.class)).thenReturn(Flux.fromIterable(expectedVehicles));

        Flux<VehicleResponse> result = commandServiceUseCase.getVehiclesByBrand(brandName);

        StepVerifier.create(result)
                .expectNextSequence(expectedVehicles)
                .verifyComplete();

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(any(Function.class));
        verify(requestBodySpec).accept(any());
    }

    @Test
    void getVehiclesByBrand_ShouldHandleServerError() {
        String brandName = "Acura";

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(VehicleResponse.class)).thenReturn(Flux.error(new RuntimeException("Server error")));

        Flux<VehicleResponse> result = commandServiceUseCase.getVehiclesByBrand(brandName);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(any(Function.class));
    }

    @Test
    void handleErrorResponse_ShouldReturnFormattedError() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/v1/brands")).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<? extends Throwable>> errorHandler = invocation.getArgument(1);
            return responseSpec;
        });
        when(responseSpec.bodyToFlux(BrandResponse.class)).thenReturn(Flux.error(new RuntimeException("Error retrieving brands - Status: 400, Body: Error details")));

        Flux<BrandResponse> result = commandServiceUseCase.getBrands();

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Error retrieving brands") &&
                                throwable.getMessage().contains("400") &&
                                throwable.getMessage().contains("Error details"))
                .verify();
    }

    @Test
    void handleErrorResponse_ShouldHandleEmptyErrorBody() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/v1/brands")).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.onStatus(any(), any())).thenAnswer(invocation -> {
            Function<ClientResponse, Mono<? extends Throwable>> errorHandler = invocation.getArgument(1);
            return responseSpec;
        });
        when(responseSpec.bodyToFlux(BrandResponse.class)).thenReturn(Flux.error(new RuntimeException("Server error while retrieving brands - Status: 500, Body: No error body")));

        Flux<BrandResponse> result = commandServiceUseCase.getBrands();

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().contains("Server error while retrieving brands") &&
                                throwable.getMessage().contains("500") &&
                                throwable.getMessage().contains("No error body"))
                .verify();
    }

    @Test
    void getVehiclesByBrand_WithQueryParam_ShouldBuildCorrectUri() {
        String brandName = "Honda";
        List<VehicleResponse> expectedVehicles = List.of(
                new VehicleResponse(1L, "001234-1", "1", "Civic", "Compact car")
        );

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.accept(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(VehicleResponse.class)).thenReturn(Flux.fromIterable(expectedVehicles));

        Flux<VehicleResponse> result = commandServiceUseCase.getVehiclesByBrand(brandName);

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

        verify(webClient).get();
        verify(requestHeadersUriSpec).uri(any(Function.class));
    }
}