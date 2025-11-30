package com.fipe.processor.application.ports.output;

import com.fipe.processor.domain.entities.Vehicle;
import reactor.core.publisher.Flux;

public interface FipeServicePort {


    Flux<Vehicle> fetchVehiclesByBrand(String brandCode);
}
