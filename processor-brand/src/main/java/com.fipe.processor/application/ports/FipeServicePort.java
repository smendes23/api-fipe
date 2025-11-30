package com.fipe.processor.application.ports;

import com.fipe.processor.domain.entities.Brand;
import reactor.core.publisher.Flux;


public interface FipeServicePort {

    Flux<Brand> fetchBrands();
}
