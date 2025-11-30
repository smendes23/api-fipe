package br.com.fipe.gateway.application.ports;

import br.com.fipe.gateway.presentation.dto.request.UpdateVehicleRequest;
import br.com.fipe.gateway.presentation.dto.response.BrandResponse;
import br.com.fipe.gateway.presentation.dto.response.VehicleResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommandServicePort {

    Mono<String> dataLoad();
    Flux<BrandResponse> getBrands();
    Flux<VehicleResponse> getVehiclesByBrand(String brandCode);
    Mono<VehicleResponse> updateVehicle(Long id, UpdateVehicleRequest request);
}
