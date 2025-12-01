package com.fipe.processor.application.usecases;

import com.fipe.processor.application.ports.CacheServicePort;
import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.domain.repositories.BrandRepository;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetBrandsUseCaseTest {

    @Mock
    private CacheServicePort cacheService;

    @Mock
    private BrandRepository brandRepository;

    @InjectMocks
    private GetBrandsUseCase getBrandsUseCase;

    GetBrandsUseCaseTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should retrieve brands from cache when available")
    void shouldRetrieveBrandsFromCache() {
        Brand brand1 = Brand.create("001", "Brand A");
        Brand brand2 = Brand.create("002", "Brand B");
        when(brandRepository.findAllByOrderByNameAsc()).thenReturn(Flux.fromIterable(Arrays.asList(brand1,brand2)));
        when(cacheService.get(eq("brands:all"), eq(Brand[].class)))
                .thenReturn(Mono.just(new Brand[]{brand1, brand2}));

        Flux<Brand> result = getBrandsUseCase.execute();

        StepVerifier.create(result)
                .expectNext(brand1, brand2)
                .verifyComplete();
        verify(cacheService, times(1)).get(eq("brands:all"), eq(Brand[].class));
    }

    @Test
    @DisplayName("Should retrieve brands from database and update cache when cache is empty")
    void shouldRetrieveBrandsFromDatabaseAndUpdateCache() {
        Brand brand1 = Brand.create("001", "Brand A");
        Brand brand2 = Brand.create("002", "Brand B");
        List<Brand> brands = Arrays.asList(brand1, brand2);
        when(cacheService.get(eq("brands:all"), eq(Brand[].class))).thenReturn(Mono.empty());
        when(brandRepository.findAllByOrderByNameAsc()).thenReturn(Flux.fromIterable(brands));
        when(cacheService.put(eq("brands:all"), eq(brands), eq(Duration.ofHours(1)))).thenReturn(Mono.empty());

        Flux<Brand> result = getBrandsUseCase.execute();

        StepVerifier.create(result)
                .expectNext(brand1, brand2)
                .verifyComplete();
        verify(cacheService, times(1)).get(eq("brands:all"), eq(Brand[].class));
        verify(brandRepository, times(1)).findAllByOrderByNameAsc();
        verify(cacheService, times(1)).put(eq("brands:all"), eq(brands), eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("Should handle error when retrieving data from cache and fallback to database")
    void shouldHandleCacheErrorAndFallbackToDatabase() {
        Brand brand1 = Brand.create("001", "Brand A");
        Brand brand2 = Brand.create("002", "Brand B");
        List<Brand> brands = Arrays.asList(brand1, brand2);
        when(cacheService.get(eq("brands:all"), eq(Brand[].class)))
                .thenReturn(Mono.error(new RuntimeException("Cache error")));
        when(brandRepository.findAllByOrderByNameAsc()).thenReturn(Flux.fromIterable(brands));
        when(cacheService.put(eq("brands:all"), eq(brands), eq(Duration.ofHours(1)))).thenReturn(Mono.empty());

        Flux<Brand> result = getBrandsUseCase.execute();

        StepVerifier.create(result)
                .expectNext(brand1, brand2)
                .verifyComplete();
        verify(cacheService, times(1)).get(eq("brands:all"), eq(Brand[].class));
        verify(brandRepository, times(1)).findAllByOrderByNameAsc();
        verify(cacheService, times(1)).put(eq("brands:all"), eq(brands), eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("Should return empty result when no brands are found in database")
    void shouldReturnEmptyResultWhenNoBrandsFound() {
        when(cacheService.get(eq("brands:all"), eq(Brand[].class))).thenReturn(Mono.empty());
        when(brandRepository.findAllByOrderByNameAsc()).thenReturn(Flux.empty());

        Flux<Brand> result = getBrandsUseCase.execute();

        StepVerifier.create(result)
                .verifyComplete();
        verify(cacheService, times(1)).get(eq("brands:all"), eq(Brand[].class));
        verify(brandRepository, times(1)).findAllByOrderByNameAsc();
        verify(cacheService, never()).put(any(), any(), any());
    }
}