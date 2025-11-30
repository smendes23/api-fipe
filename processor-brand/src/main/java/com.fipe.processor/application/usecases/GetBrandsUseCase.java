package com.fipe.processor.application.usecases;

import com.fipe.processor.application.ports.CacheServicePort;
import com.fipe.processor.application.ports.GetBrandsServicePort;
import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.domain.repositories.BrandRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetBrandsUseCase implements GetBrandsServicePort {

    private final BrandRepository brandRepository;
    private final CacheServicePort cacheService;

    private static final String CACHE_KEY = "brands:all";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Override
    public Flux<Brand> execute() {
        log.debug("Retrieving all brands with cache aside strategy");

        return getFromCache()
                .switchIfEmpty(getFromDatabaseAndUpdateCache())
                .doOnComplete(() -> log.debug("Brands retrieval completed"))
                .doOnError(error -> log.error("Error retrieving brands: {}", error.getMessage()));
    }

    @Override
    public Mono<Void> clearCache() {
        log.debug("Clearing brands cache");
        return cacheService.delete(CACHE_KEY);
    }

    private Flux<Brand> getFromCache() {
        return cacheService.get(CACHE_KEY, Brand[].class)
                .flatMapMany(Flux::fromArray)
                .doOnNext(brands -> log.debug("Cache hit for brands"))
                .onErrorResume(error -> {
                    log.warn("Error reading from cache, falling back to database: {}", error.getMessage());
                    return Flux.empty();
                });
    }

    private Flux<Brand> getFromDatabaseAndUpdateCache() {
        return brandRepository.findAllByOrderByNameAsc()
                .collectList()
                .flatMap(brands -> {
                    if (!brands.isEmpty()) {
                        return cacheService.put(CACHE_KEY, brands, CACHE_TTL)
                                .thenReturn(brands);
                    }
                    return Mono.just(brands);
                })
                .flatMapMany(Flux::fromIterable)
                .doOnNext(brands -> log.debug("Retrieved brands from database and updated cache"))
                .onErrorResume(error -> {
                    log.error("Error updating cache, returning database results: {}", error.getMessage());
                    return brandRepository.findAllByOrderByNameAsc();
                });
    }
}
