package com.fipe.processor.presentation.mappers;

import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.domain.entities.Vehicle;
import com.fipe.processor.presentation.dto.BrandResponse;
import com.fipe.processor.presentation.dto.VehicleResponse;
import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    public BrandResponse toBrandResponse(Brand brand) {
        return new BrandResponse(brand.getCode(), brand.getName());
    }

    public VehicleResponse toVehicleResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getCode(),
                vehicle.getBrandCode(),
                vehicle.getModel(),
                vehicle.getObservations()
        );
    }
}
