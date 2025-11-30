package com.fipe.processor.domain.repositories;

import com.fipe.processor.domain.entities.Vehicle;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository interface for Vehicle entity in the processor service.
 * 
 * This interface follows the Repository Pattern from DDD, providing
 * an abstraction over data access.
 * 
 * @author FIPE Team
 * @version 1.0.0
 */
@Repository
public interface VehicleRepository extends R2dbcRepository<Vehicle, Long> {

    /**
     * Finds a vehicle by code and brand code.
     * 
     * @param code the vehicle code
     * @param brandCode the brand code
     * @return a Mono of the vehicle if found
     */
    Mono<Vehicle> findByCodeAndBrandCode(String code, String brandCode);

    /**
     * Checks if a vehicle exists by code and brand code.
     * 
     * @param code the vehicle code
     * @param brandCode the brand code
     * @return a Mono of true if exists, false otherwise
     */
    Mono<Boolean> existsByCodeAndBrandCode(String code, String brandCode);
}
