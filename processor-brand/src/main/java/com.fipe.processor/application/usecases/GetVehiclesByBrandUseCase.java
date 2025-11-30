package com.fipe.processor.application.usecases;

import com.fipe.processor.application.ports.CacheServicePort;
import com.fipe.processor.application.ports.GetVehiclesByBrandServicePort;
import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.domain.repositories.BrandRepository;
import com.fipe.processor.domain.repositories.VehicleRepository;
import com.fipe.processor.presentation.exception.BrandNotFoundException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetVehiclesByBrandUseCase implements GetVehiclesByBrandServicePort {

    private final VehicleRepository vehicleRepository;
    private final BrandRepository brandRepository;
    private final CacheServicePort cacheService;

    private static final String CACHE_KEY_PREFIX = "vehicles:brand:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public Flux<Vehicle> execute(final String brandName) {
        log.debug("Retrieving vehicles for brand: {} with cache aside strategy", brandName);

        String cacheKey = buildCacheKey(brandName);

        return getFromCache(cacheKey)
                .switchIfEmpty(getFromDatabaseAndUpdateCache(brandName, cacheKey))
                .doOnComplete(() -> log.debug("Vehicles retrieval completed for brand: {}", brandName))
                .doOnError(error -> log.error("Error retrieving vehicles for brand {}: {}", brandName, error.getMessage()));
    }

    @Override
    public Mono<Void> clearCacheByBrand(String brandName) {
        log.debug("Clearing vehicles cache for brand: {}", brandName);
        String cacheKey = buildCacheKey(brandName);
        return cacheService.delete(cacheKey);
    }

    private String buildCacheKey(String brandName) {
        return CACHE_KEY_PREFIX + brandName.toLowerCase().replace(" ", "_");
    }

    private Flux<Vehicle> getFromCache(String cacheKey) {
        return cacheService.get(cacheKey, Vehicle[].class)
                .flatMapMany(Flux::fromArray)
                .doOnNext(vehicles -> log.debug("Cache hit for vehicles of brand: {}", cacheKey))
                .onErrorResume(error -> {
                    log.warn("Error reading from cache for key {}, falling back to database: {}",
                            cacheKey, error.getMessage());
                    return Flux.empty();
                });
    }

    private Flux<Vehicle> getFromDatabaseAndUpdateCache(String brandName, String cacheKey) {
        return vehicleRepository.findByBrandName(brandName, brandRepository)
                .collectList()
                .flatMap(vehicles -> {
                    if (!vehicles.isEmpty()) {
                        return cacheService.put(cacheKey, vehicles, CACHE_TTL)
                                .thenReturn(vehicles);
                    }
                    return Mono.just(vehicles);
                })
                .flatMapMany(Flux::fromIterable)
                .doOnNext(vehicles -> log.debug("Retrieved vehicles from database and updated cache for brand: {}", brandName))
                .onErrorResume(error -> {
                    log.error("Error updating cache for brand {}, returning database results: {}",
                            brandName, error.getMessage());
                    return vehicleRepository.findByBrandName(brandName, brandRepository);
                })
                .switchIfEmpty(Mono.error(new BrandNotFoundException("Brand not found: " + brandName)));
    }
}
