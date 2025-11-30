package com.fipe.processor.infrastructure.adapters.input.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fipe.processor.application.usecases.ProcessBrandUseCase;
import com.fipe.processor.domain.MessageProcessingException;
import com.fipe.processor.domain.entities.Brand;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrandMessageConsumer {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ProcessBrandUseCase processBrandUseCase;
    private final ObjectMapper objectMapper;
    private final RateLimiterRegistry rateLimiterRegistry;

    @Value("${kafka.topics.brands}")
    private String brandsTopic;

    private RateLimiter fipeRateLimiter;
    private Disposable subscription;

    @PostConstruct
    public void startConsuming() {
        this.fipeRateLimiter = rateLimiterRegistry.rateLimiter("fipeApiLimiter");

        log.info("Starting Kafka consumer for topic: {}", brandsTopic);

        this.subscription = kafkaReceiver.receive()
                .publishOn(Schedulers.boundedElastic()) // Processamento fora da thread do Kafka
                .doOnNext(record -> log.debug("Received message key: {}, offset: {}",
                        record.key(), record.offset()))
                .concatMap(this::processMessageWithRetry)
                .doOnError(error -> log.error("Critical error in Kafka consumer stream: {}", error.getMessage()))
                .retryWhen(Retry.backoff(10, Duration.ofSeconds(5))
                        .maxBackoff(Duration.ofMinutes(5))
                        .jitter(0.5)
                        .doBeforeRetry(retry -> log.warn("Retrying Kafka stream after error: {}",
                                retry.failure().getMessage()))
                )
                .subscribe(
                        null,
                        error -> log.error("Fatal error in Kafka consumer", error),
                        () -> log.info("Kafka consumer completed")
                );

        log.info("Kafka consumer started successfully");
    }

    Mono<Void> processMessageWithRetry(ReceiverRecord<String, String> record) {
        return Mono.defer(() -> processSingleMessage(record))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableError)
                        .doBeforeRetry(retry -> {
                            log.warn("Retry attempt {} for brand {}: {}",
                                    retry.totalRetries() + 1,
                                    record.key(),
                                    retry.failure().getMessage());
                        })
                )
                .onErrorResume(error -> handlePermanentError(record, error))
                .timeout(Duration.ofMinutes(10)) // Timeout de 10 minutos por mensagem
                .doOnSuccess(v -> log.debug("Successfully processed brand: {}", record.key()))
                .doOnError(error -> log.error("Failed to process brand {}: {}", record.key(), error.getMessage()));
    }

    private Mono<Void> processSingleMessage(ReceiverRecord<String, String> record) {
        return Mono.fromCallable(() -> {
                    try {
                        Brand brandMessage = objectMapper.readValue(record.value(), Brand.class);
                        log.info("Processing brand: {} - Offset: {}", record.key(), record.offset());
                        return brandMessage;
                    } catch (Exception e) {
                        log.error("Error deserializing message for brand {}: {}", record.key(), e.getMessage());
                        throw new MessageProcessingException("Invalid message format", e, false); // Não retryable
                    }
                })
                .transformDeferred(RateLimiterOperator.of(fipeRateLimiter))
                .flatMap(brand -> processBrandUseCase.execute(brand)
                        .collectList()
                        .doOnSuccess(vehicles -> log.info("Successfully processed brand {}: {} vehicles",
                                brand.getCode(), vehicles.size()))
                )
                .flatMap(ignore -> commitOffset(record))
                .onErrorResume(error -> handleProcessingError(record, error));
    }

    private Mono<Void> commitOffset(ReceiverRecord<String, String> record) {
        return record.receiverOffset()
                .commit()
                .doOnSuccess(v -> log.debug("Committed offset for brand {} at offset {}",
                        record.key(), record.offset()))
                .doOnError(error -> log.error("Failed to commit offset for brand {} at offset {}: {}",
                        record.key(), record.offset(), error.getMessage()));
    }

    private Mono<Void> handleProcessingError(ReceiverRecord<String, String> record, Throwable error) {
        String brandCode = record.key();

        if (error instanceof MessageProcessingException mpe && !mpe.isRetryable()) {
            log.warn("Non-retryable error for brand {}: {}. Committing offset.",
                    brandCode, error.getMessage());
            return commitOffset(record)
                    .doOnSuccess(v -> log.info("Committed non-retryable message for brand {}", brandCode));
        }

        if (!isRetryableError(error)) {
            log.error("Permanent error for brand {} at offset {}: {}",
                    brandCode, record.offset(), error.getMessage());
            return commitOffset(record)
                    .doOnSuccess(v -> log.warn("Committed permanently failed message for brand {}", brandCode));
        }

        log.warn("Retryable error for brand {}: {}. Offset NOT committed.",
                brandCode, error.getMessage());
        return Mono.error(error);
    }

    private Mono<Void> handlePermanentError(ReceiverRecord<String, String> record, Throwable error) {
        log.error("Permanent error after retries for brand {} at offset {}: {}",
                record.key(), record.offset(), error.getMessage());

        return commitOffset(record)
                .doOnSuccess(v -> log.warn("Committed message after permanent failure for brand {}", record.key()));
    }

    private boolean isRetryableError(Throwable error) {
        if (error instanceof MessageProcessingException mpe) {
            return mpe.isRetryable();
        }

        return error instanceof TimeoutException ||
                error instanceof WebClientResponseException.TooManyRequests ||
                error instanceof WebClientResponseException.ServiceUnavailable ||
                error instanceof WebClientResponseException.GatewayTimeout ||
                error instanceof IOException ||
                (error.getMessage() != null &&
                        (error.getMessage().contains("timeout") ||
                                error.getMessage().contains("rate limit") ||
                                error.getMessage().contains("temporarily") ||
                                error.getMessage().contains("connection")));
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down Kafka consumer");
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("Kafka consumer subscription disposed");
        }
    }


}
