package com.fipe.processor.application.usecases;

import com.fipe.processor.application.ports.BrandMessagePublisherPort;
import com.fipe.processor.application.ports.FipeServicePort;
import com.fipe.processor.application.ports.LoadFipeDataPort;
import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.domain.repositories.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Slf4j
@Service
@RequiredArgsConstructor
public class LoadFipeDataUseCase  implements LoadFipeDataPort {

    private final FipeServicePort fipeService;
    private final BrandRepository brandRepository;
    private final BrandMessagePublisherPort messagePublisher;

    @Value("${kafka.topics.brands}")
    private String brandsTopic;

    public Flux<Brand> execute() {
        log.info("Starting FIPE data load process");

        return fipeService.fetchBrands()
                .flatMap(this::saveBrand)
                .flatMap(this::publishBrandMessage)
                .doOnComplete(() -> log.info("FIPE data load process completed"))
                .doOnError(error -> log.error("Error during FIPE data load: {}", error.getMessage()));
    }

    private Mono<Brand> saveBrand(final Brand brand) {
        log.debug("Saving brand: {} - {}", brand.getCode(), brand.getName());
        
        return brandRepository.save(brand)
                .doOnSuccess(saved -> log.debug("Brand saved: {}", saved.getCode()))
                .doOnError(error -> log.error("Error saving brand {}: {}", 
                        brand.getCode(), error.getMessage()));
    }

    private Mono<Brand> publishBrandMessage(final Brand brand) {
        log.debug("Publishing brand message: {}", brand.getCode());
        
        return messagePublisher.publish(brandsTopic, brand.getCode(), brand)
                .thenReturn(brand)
                .doOnSuccess(b -> log.debug("Brand message published: {}", b.getCode()))
                .doOnError(error -> log.error("Error publishing brand {}: {}", 
                        brand.getCode(), error.getMessage()));
    }
}
