package com.fipe.processor.domain.repositories;

import com.fipe.processor.domain.entities.Vehicle;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface VehicleRepository extends R2dbcRepository<Vehicle, Long> {

    Mono<Boolean> existsByCodeAndBrandCode(String code, String brandCode);
}
