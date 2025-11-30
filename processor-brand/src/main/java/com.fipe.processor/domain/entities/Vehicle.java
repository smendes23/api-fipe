package com.fipe.processor.domain.entities;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("vehicles")
public class Vehicle {

    @Id
    private Long id;

    private String code;

    private String brandCode;

    private String model;

    private String observations;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

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

    public void update(String newModel, String newObservations) {
        if (newModel != null && !newModel.isBlank()) {
            this.model = newModel;
        }
        this.observations = newObservations;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isValid() {
        return id != null
                && code != null && !code.isBlank()
                && brandCode != null && !brandCode.isBlank()
                && model != null && !model.isBlank();
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", brandCode='" + brandCode + '\'' +
                ", model='" + model + '\'' +
                ", observations='" + observations + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}