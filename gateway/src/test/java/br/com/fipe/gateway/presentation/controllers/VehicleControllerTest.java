package br.com.fipe.gateway.presentation.controllers;

import br.com.fipe.gateway.application.ports.input.CommandServicePort;
import br.com.fipe.gateway.config.TestSecurityConfig;
import br.com.fipe.gateway.config.TestValidationConfig;
import br.com.fipe.gateway.presentation.dto.request.UpdateVehicleRequest;
import br.com.fipe.gateway.presentation.dto.response.BrandResponse;
import br.com.fipe.gateway.presentation.dto.response.VehicleResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(VehicleController.class)
@ExtendWith(MockitoExtension.class)
@Import({TestSecurityConfig.class, TestValidationConfig.class})
class VehicleControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CommandServicePort commandServicePort;

    private BrandResponse brandResponse1;
    private BrandResponse brandResponse2;
    private VehicleResponse vehicleResponse1;
    private VehicleResponse vehicleResponse2;
    private UpdateVehicleRequest updateVehicleRequest;

    @BeforeEach
    void setUp() {
        brandResponse1 = new BrandResponse("1", "Acura");
        brandResponse2 = new BrandResponse("2", "Audi");

        vehicleResponse1 = new VehicleResponse(1L, "001004-1", "1", "Integra GS 1.8", "Imported vehicle");
        vehicleResponse2 = new VehicleResponse(2L, "001005-1", "1", "NSX", "Sports car");

        updateVehicleRequest = new UpdateVehicleRequest("Integra GS 1.8 Special Edition", "Imported with custom modifications");
    }

    @Test
    @WithMockUser
    void loadData_ShouldReturnAccepted() {
        String expectedResponse = "Data load initiated successfully";
        when(commandServicePort.dataLoad()).thenReturn(Mono.just(expectedResponse));

        webTestClient.post()
                .uri("/api/v1/vehicles/load")
                .exchange()
                .expectStatus().isAccepted()
                .expectBody(String.class)
                .isEqualTo(expectedResponse);

        verify(commandServicePort).dataLoad();
    }

    @Test
    @WithMockUser
    void loadData_ShouldHandleServiceError() {
        when(commandServicePort.dataLoad()).thenReturn(Mono.error(new RuntimeException("Service error")));

        webTestClient.post()
                .uri("/api/v1/vehicles/load")
                .exchange()
                .expectStatus().is5xxServerError();

        verify(commandServicePort).dataLoad();
    }

    @Test
    @WithMockUser
    void getBrands_ShouldReturnBrandsList() {
        List<BrandResponse> expectedBrands = List.of(brandResponse1, brandResponse2);
        when(commandServicePort.getBrands()).thenReturn(Flux.fromIterable(expectedBrands));

        webTestClient.get()
                .uri("/api/v1/brands")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(BrandResponse.class)
                .hasSize(2)
                .contains(brandResponse1, brandResponse2);

        verify(commandServicePort).getBrands();
    }

    @Test
    @WithMockUser
    void getBrands_ShouldHandleEmptyList() {
        when(commandServicePort.getBrands()).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/brands")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(BrandResponse.class)
                .hasSize(0);

        verify(commandServicePort).getBrands();
    }

    @Test
    @WithMockUser
    void getBrands_ShouldHandleServiceError() {
        when(commandServicePort.getBrands()).thenReturn(Flux.error(new RuntimeException("Database error")));

        webTestClient.get()
                .uri("/api/v1/brands")
                .exchange()
                .expectStatus().is5xxServerError();

        verify(commandServicePort).getBrands();
    }

    @Test
    @WithMockUser
    void getVehiclesByBrand_ShouldReturnVehicles() {
        String brandName = "Acura";
        List<VehicleResponse> expectedVehicles = List.of(vehicleResponse1, vehicleResponse2);
        when(commandServicePort.getVehiclesByBrand(brandName)).thenReturn(Flux.fromIterable(expectedVehicles));

        webTestClient.get()
                .uri("/api/v1/vehicles/{brandName}", brandName)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(VehicleResponse.class)
                .hasSize(2)
                .contains(vehicleResponse1, vehicleResponse2);

        verify(commandServicePort).getVehiclesByBrand(brandName);
    }

    @Test
    @WithMockUser
    void getVehiclesByBrand_ShouldHandleEmptyList() {
        String brandName = "UnknownBrand";
        when(commandServicePort.getVehiclesByBrand(brandName)).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/vehicles/{brandName}", brandName)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(VehicleResponse.class)
                .hasSize(0);

        verify(commandServicePort).getVehiclesByBrand(brandName);
    }

    @Test
    @WithMockUser
    void getVehiclesByBrand_ShouldHandleServiceError() {
        String brandName = "Acura";
        when(commandServicePort.getVehiclesByBrand(brandName)).thenReturn(Flux.error(new RuntimeException("Brand not found")));

        webTestClient.get()
                .uri("/api/v1/vehicles/{brandName}", brandName)
                .exchange()
                .expectStatus().is5xxServerError();

        verify(commandServicePort).getVehiclesByBrand(brandName);
    }

    @Test
    @WithMockUser
    void updateVehicle_ShouldReturnUpdatedVehicle() {
        Long vehicleId = 1L;
        VehicleResponse expectedResponse = new VehicleResponse(vehicleId, "001004-1", "1", "Integra GS 1.8 Special Edition", "Imported with custom modifications");

        when(commandServicePort.updateVehicle(anyLong(), any(UpdateVehicleRequest.class)))
                .thenReturn(Mono.just(expectedResponse));

        webTestClient.put()
                .uri("/api/v1/vehicles/{id}", vehicleId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateVehicleRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(VehicleResponse.class)
                .isEqualTo(expectedResponse);

        verify(commandServicePort).updateVehicle(vehicleId, updateVehicleRequest);
    }

    @Test
    @WithMockUser
    void updateVehicle_ShouldHandleNotFound() {
        Long vehicleId = 999L;
        when(commandServicePort.updateVehicle(anyLong(), any(UpdateVehicleRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Vehicle not found")));

        webTestClient.put()
                .uri("/api/v1/vehicles/{id}", vehicleId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateVehicleRequest)
                .exchange()
                .expectStatus().is5xxServerError();

        verify(commandServicePort).updateVehicle(vehicleId, updateVehicleRequest);
    }

    @Test
    void loadData_WithoutAuthentication_ShouldReturnUnauthorized() {
        webTestClient.post()
                .uri("/api/v1/vehicles/load")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getBrands_WithoutAuthentication_ShouldReturnUnauthorized() {
        webTestClient.get()
                .uri("/api/v1/brands")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}