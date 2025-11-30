package com.fipe.processor.application.usecases;

import com.fipe.processor.application.ports.BrandMessagePublisherPort;
import com.fipe.processor.application.ports.FipeServicePort;
import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.domain.repositories.BrandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadFipeDataUseCaseTest {

    @Mock
    private FipeServicePort fipeService;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private BrandMessagePublisherPort messagePublisher;

    @InjectMocks
    private LoadFipeDataUseCase loadFipeDataUseCase;

    
    @Test
    void shouldSuccessfullyLoadSaveAndPublishBrands() {
        
        Brand brand1 = Brand.create("001", "Brand One");
        Brand brand2 = Brand.create("002", "Brand Two");

        when(fipeService.fetchBrands()).thenReturn(Flux.just(brand1, brand2));
        when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(messagePublisher.publish(any(), any(), any())).thenReturn(Mono.empty());

        
        StepVerifier.create(loadFipeDataUseCase.execute())
                .expectNext(brand1, brand2)
                .verifyComplete();

        
        verify(fipeService).fetchBrands();
        verify(brandRepository, times(2)).save(any(Brand.class));
        verify(messagePublisher, times(2)).publish(any(), any(), any());
    }

    @Test
    void shouldHandleSaveBrandError() {
        
        Brand brand1 = Brand.create("001", "Brand One");

        when(fipeService.fetchBrands()).thenReturn(Flux.just(brand1));
        when(brandRepository.save(any(Brand.class))).thenReturn(Mono.error(new RuntimeException("Save error")));

        
        StepVerifier.create(loadFipeDataUseCase.execute())
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        "Save error".equals(throwable.getMessage()))
                .verify();

        
        verify(fipeService).fetchBrands();
        verify(brandRepository).save(any(Brand.class));
        verifyNoInteractions(messagePublisher);
    }

    @Test
    void shouldHandlePublishBrandMessageError() {
        
        Brand brand1 = Brand.create("001", "Brand One");

        when(fipeService.fetchBrands()).thenReturn(Flux.just(brand1));
        when(brandRepository.save(any(Brand.class))).thenReturn(Mono.just(brand1));
        when(messagePublisher.publish(any(), any(), any())).thenReturn(Mono.error(new RuntimeException("Publish error")));

        
        StepVerifier.create(loadFipeDataUseCase.execute())
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        "Publish error".equals(throwable.getMessage()))
                .verify();

        
        verify(fipeService).fetchBrands();
        verify(brandRepository).save(any(Brand.class));
        verify(messagePublisher).publish(any(), any(), any());
    }

    @Test
    void shouldHandleEmptyBrandList() {
        
        when(fipeService.fetchBrands()).thenReturn(Flux.empty());

        
        StepVerifier.create(loadFipeDataUseCase.execute())
                .verifyComplete();

        
        verify(fipeService).fetchBrands();
        verifyNoInteractions(brandRepository);
        verifyNoInteractions(messagePublisher);
    }

    @Test
    void shouldHandleFipeServiceError() {
        
        when(fipeService.fetchBrands()).thenReturn(Flux.error(new RuntimeException("Service unavailable")));

        
        StepVerifier.create(loadFipeDataUseCase.execute())
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        "Service unavailable".equals(throwable.getMessage()))
                .verify();

        
        verify(fipeService).fetchBrands();
        verifyNoInteractions(brandRepository);
        verifyNoInteractions(messagePublisher);
    }
}