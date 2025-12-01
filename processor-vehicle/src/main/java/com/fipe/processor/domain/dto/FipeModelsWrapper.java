package com.fipe.processor.domain.dto;

import com.fipe.processor.infrastructure.adapters.output.dto.FipeVehicleResponse;
import java.util.List;

public record FipeModelsWrapper(List<FipeVehicleResponse> modelos) {}
