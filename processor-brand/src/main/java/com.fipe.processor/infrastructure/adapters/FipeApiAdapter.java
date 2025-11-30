package com.fipe.processor.infrastructure.adapters;

import com.fipe.processor.application.ports.FipeServicePort;
import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.infrastructure.adapters.dto.FipeBrandResponse;
import com.fipe.processor.infrastructure.adapters.mappers.DomainMapper;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class FipeApiAdapter implements FipeServicePort {

    private final WebClient webClient;

    @Value("${fipe.api.timeout:30000}")
    private int timeout;

    @Value("${fipe.api.max-retries:2}")
    private int maxRetries;

    @Override
    public Flux<Brand> fetchBrands() {
        return webClient.get()
                .uri("/carros/marcas")
                .retrieve()
                .bodyToFlux(FipeBrandResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .doBeforeRetry(signal -> log.warn("Retrying FIPE API call, attempt: {}",
                                signal.totalRetries() + 1)))
                .map(DomainMapper::mapToDomain)
                .doOnComplete(() -> log.info("Successfully fetched brands from FIPE API"))
                .doOnError(error -> log.error("Error fetching brands from FIPE API: {}",
                        error.getMessage()));
    }
}
