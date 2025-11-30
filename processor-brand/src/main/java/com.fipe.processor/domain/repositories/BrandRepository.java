package com.fipe.processor.domain.repositories;

import com.fipe.processor.domain.entities.Brand;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface BrandRepository extends R2dbcRepository<Brand, Long> {
    Flux<Brand> findAllByOrderByNameAsc();
    @Query("SELECT code FROM brands WHERE name = :name")
    Mono<String> findCodeByName(@Param("name") String name);
}
