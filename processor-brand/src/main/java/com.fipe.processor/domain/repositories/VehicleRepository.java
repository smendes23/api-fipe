package com.fipe.processor.domain.repositories;

import com.fipe.processor.domain.entities.Vehicle;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface VehicleRepository extends R2dbcRepository<Vehicle, Long> {

    Flux<Vehicle> findByBrandCodeOrderByModelAsc(String brandCode);

    default Flux<Vehicle> findByBrandName(String brandName, BrandRepository brandRepository) {
        return brandRepository.findCodeByName(brandName)
                .flatMapMany(this::findByBrandCodeOrderByModelAsc);
    }
}
