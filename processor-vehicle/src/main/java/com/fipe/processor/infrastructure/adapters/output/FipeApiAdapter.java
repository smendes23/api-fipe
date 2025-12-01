package com.fipe.processor.infrastructure.adapters.output;

import com.fipe.processor.application.ports.output.FipeServicePort;
import com.fipe.processor.domain.dto.FipeModelsWrapper;
import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.infrastructure.adapters.output.dto.FipeVehicleResponse;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class FipeApiAdapter implements FipeServicePort {
    private static final String USER_AGENT = "MyApp/1.0 (+http://myapp.com)";

    private final WebClient webClient;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());
    private final int MAX_REQUESTS_PER_DAY = 450; // Margem de segurança

    @Value("${fipe.api.base-url}")
    private String baseUrl;

    @Value("${fipe.api.timeout:30000}")
    private int timeout;

    @Value("${fipe.api.max-retries:2}")
    private int maxRetries;

    @Override
    public Flux<Vehicle> fetchVehiclesByBrand(String brandCode) {
        log.info("Fetching vehicles for brand {} from FIPE API", brandCode);

        return Mono.fromCallable(() -> brandCode)
                // Rate limiting baseado no limite diário
                .doOnNext(brand -> checkRateLimit())
                .flatMapMany(this::makeApiCallWithRetry)
                .doOnComplete(() -> log.info("Successfully fetched vehicles for brand {}", brandCode))
                .doOnError(error -> log.error("Error fetching vehicles for brand {}: {}", brandCode, error.getMessage()));
    }

    void checkRateLimit() {
        long now = System.currentTimeMillis();
        long lastReset = lastResetTime.get();

        if (now - lastReset > 24 * 60 * 60 * 1000) {
            requestCount.set(0);
            lastResetTime.set(now);
            log.info("Rate limit reset - new day started");
        }

        int currentCount = requestCount.incrementAndGet();

        if (currentCount > MAX_REQUESTS_PER_DAY) {
            long timeUntilReset = (lastReset + 24 * 60 * 60 * 1000) - now;
            long hoursUntilReset = timeUntilReset / (60 * 60 * 1000);

            log.warn("Daily rate limit exceeded: {}/{}. Waiting {} hours until reset",
                    currentCount, MAX_REQUESTS_PER_DAY, hoursUntilReset);

            throw new RuntimeException("Daily rate limit exceeded. Try again in " + hoursUntilReset + " hours");
        }

        log.debug("Request count: {}/{}", currentCount, MAX_REQUESTS_PER_DAY);

        try {
            Thread.sleep(1000 + new Random().nextInt(2000)); // 1-3 segundos entre requests
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    Flux<Vehicle> makeApiCallWithRetry(String brandCode) {
        return makeApiCall(brandCode)
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(5))
                        .jitter(0.5)
                        .filter(this::shouldRetry)
                        .doBeforeRetry(signal -> {
                            log.warn("Retry {}/{} for brand {} after error: {}",
                                    signal.totalRetries() + 1, maxRetries, brandCode,
                                    signal.failure().getMessage());
                        })
                );
    }

    Flux<Vehicle> makeApiCall(String brandCode) {
        return webClient.get()
                .uri("/carros/marcas/{brandCode}/modelos", brandCode)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .retrieve()
                .onStatus(status -> status.value() == 429, response -> {
                    log.warn("Received 429 for brand {}. Headers: {}", brandCode, response.headers().asHttpHeaders());
                    return Mono.error(new RuntimeException("Rate limited by FIPE API"));
                })
                .bodyToMono(FipeModelsWrapper.class)
                .timeout(Duration.ofMillis(timeout))
                .flatMapMany(wrapper -> {
                    if (wrapper.modelos() == null || wrapper.modelos().isEmpty()) {
                        log.warn("No models found for brand {}", brandCode);
                        return Flux.empty();
                    }
                    return Flux.fromIterable(wrapper.modelos())
                            .map(response -> mapToDomain(response, brandCode));
                });
    }

    boolean shouldRetry(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            int statusCode = ex.getStatusCode().value();
            return statusCode == 429 || ex.getStatusCode().is5xxServerError();
        }
        return throwable instanceof TimeoutException;
    }

    Vehicle mapToDomain(FipeVehicleResponse response, String brandCode) {
        return Vehicle.create(response.codigo(), brandCode, response.nome());
    }
}
