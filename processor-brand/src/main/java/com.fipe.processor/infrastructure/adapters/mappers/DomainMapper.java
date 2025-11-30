package com.fipe.processor.infrastructure.adapters.mappers;

import com.fipe.processor.domain.entities.Brand;
import com.fipe.processor.infrastructure.adapters.dto.FipeBrandResponse;

public class DomainMapper {

    public static Brand mapToDomain(FipeBrandResponse response) {
        return Brand.create(response.codigo(), response.nome());
    }
}
