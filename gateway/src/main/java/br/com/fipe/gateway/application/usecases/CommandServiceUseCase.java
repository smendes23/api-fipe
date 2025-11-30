package br.com.fipe.gateway.application.usecases;

import br.com.fipe.gateway.application.ports.CommandServicePort;
import br.com.fipe.gateway.presentation.dto.request.UpdateVehicleRequest;
import br.com.fipe.gateway.presentation.dto.response.BrandResponse;
import br.com.fipe.gateway.presentation.dto.response.VehicleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommandServiceUseCase implements CommandServicePort {

    private final WebClient webClient;

    @Override
    public Mono<String> dataLoad() {
        return webClient.post()
                .uri("/api/v1/vehicles/load")
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(res -> log.info("Successfully fetched brands from FIPE API"))
                .doOnError(error -> log.error("Error fetching brands from FIPE API: {}", error.getMessage()));
    }

    @Override
    public Flux<BrandResponse> getBrands() {
        return webClient.get()
                .uri("/api/v1/brands")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        handleErrorResponse(response, "Error retrieving brands"))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        handleErrorResponse(response, "Server error while retrieving brands"))
                .bodyToFlux(BrandResponse.class)
                .doOnSubscribe(subscription -> log.info("Starting request for get all brands"))
                .doOnComplete(() -> log.info("Request completed to all brands"))
                .doOnError(error -> log.error("Error retrieving all brands: {}", error.getMessage()));
    }

    @Override
    public Flux<VehicleResponse> getVehiclesByBrand(final String brandName) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/vehicles")
                        .queryParam("brandName", brandName)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        handleErrorResponse(response, "Error retrieving vehicles for brand: " + brandName))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        handleErrorResponse(response, "Server error while retrieving vehicles for brand: " + brandName))
                .bodyToFlux(VehicleResponse.class)
                .doOnSubscribe(subscription -> log.info("Starting request for brand: {}", brandName))
                .doOnComplete(() -> log.info("Request completed for brand: {}", brandName))
                .doOnError(error -> log.error("Error retrieving vehicles for brand {}: {}", brandName, error.getMessage()));
    }

    @Override
    public Mono<VehicleResponse> updateVehicle(final Long id, final UpdateVehicleRequest request) {
        return webClient.put()
                .uri("/api/v1/vehicles/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        handleErrorResponse(response, "Client error updating vehicle: " + id))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        handleErrorResponse(response, "Server error updating vehicle: " + id))
                .bodyToMono(VehicleResponse.class)
                .doOnSubscribe(subscription -> log.info("Updating vehicle: {}", id))
                .doOnSuccess(response -> log.info("Vehicle updated successfully: {}", id))
                .doOnError(error -> log.error("Error updating vehicle {}: {}", id, error.getMessage()));
    }

    private Mono<? extends Throwable> handleErrorResponse(ClientResponse response, String errorMessage) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("No error body")
                .flatMap(errorBody -> {
                    String fullErrorMessage = String.format("%s - Status: %d, Body: %s",
                            errorMessage, response.statusCode().value(), errorBody);
                    log.error(fullErrorMessage);
                    return Mono.error(new RuntimeException(fullErrorMessage));
                });
    }
}