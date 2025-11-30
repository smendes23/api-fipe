package br.com.fipe.gateway.domain.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Vehicle entity representing a vehicle in the domain model.
 * 
 * This is a DDD aggregate root that represents a vehicle with its brand,
 * model, and observations. It encapsulates business logic and maintains
 * consistency boundaries.
 * 
 * Follows SOLID principles:
 * - SRP: Focuses only on vehicle data and behavior
 * - OCP: Extensible through inheritance if needed
 * 
 * @author FIPE Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("vehicles")
public class Vehicle {

    /**
     * Unique identifier (UUID).
     */
    @Id
    private Long id;

    /**
     * Vehicle code from FIPE.
     */
    private String code;

    /**
     * Brand code (foreign key).
     */
    private String brandCode;

    /**
     * Vehicle model name.
     */
    private String model;

    /**
     * Additional observations about the vehicle.
     */
    private String observations;

    /**
     * Creation timestamp.
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp.
     */
    private LocalDateTime updatedAt;

    /**
     * Factory method to create a new Vehicle instance.
     * Implements the Factory Pattern for object creation.
     * 
     * @param code the vehicle code
     * @param brandCode the brand code
     * @param model the vehicle model
     * @return a new Vehicle instance
     */
    public static Vehicle create(String code, String brandCode, String model) {
        LocalDateTime now = LocalDateTime.now();
        return Vehicle.builder()
                .code(code)
                .brandCode(brandCode)
                .model(model)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Updates the vehicle model and observations.
     * Implements the business logic for updating vehicle data.
     * 
     * @param newModel the new model name
     * @param newObservations the new observations
     */
    public void update(String newModel, String newObservations) {
        if (newModel != null && !newModel.isBlank()) {
            this.model = newModel;
        }
        this.observations = newObservations;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Validates the vehicle entity.
     * 
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return id != null 
                && code != null && !code.isBlank()
                && brandCode != null && !brandCode.isBlank()
                && model != null && !model.isBlank();
    }
}
