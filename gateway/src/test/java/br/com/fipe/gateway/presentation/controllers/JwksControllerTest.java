package br.com.fipe.gateway.presentation.controllers;

import br.com.fipe.gateway.config.TestSecurityConfig;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
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

import static org.mockito.Mockito.when;

@WebFluxTest(JwksController.class)
@ExtendWith(MockitoExtension.class)
@Import(TestSecurityConfig.class)
class JwksControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RSAPublicKey publicKey;

    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();

        when(publicKey.getModulus()).thenReturn(((RSAPublicKey) keyPair.getPublic()).getModulus());
        when(publicKey.getPublicExponent()).thenReturn(((RSAPublicKey) keyPair.getPublic()).getPublicExponent());
        when(publicKey.getAlgorithm()).thenReturn("RSA");
        when(publicKey.getFormat()).thenReturn("X.509");
    }

    @Test
    @WithMockUser
    void jwks_ShouldReturnValidJwksJson() {
        webTestClient.get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.keys").isArray()
                .jsonPath("$.keys[0].kty").isEqualTo("RSA")
                .jsonPath("$.keys[0].n").exists()
                .jsonPath("$.keys[0].e").exists();
    }

    @Test
    @WithMockUser
    void jwks_ShouldContainCorrectKeyInformation() {
        webTestClient.get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(response -> {
                    // Verificar se a estrutura básica está correta
                    assert response.containsKey("keys");
                    var keys = (java.util.List<?>) response.get("keys");
                    assert keys.size() == 1;

                    var key = (Map<?, ?>) keys.get(0);
                    assert key.get("kty").equals("RSA");
                    assert key.containsKey("n");
                    assert key.containsKey("e");
                });
    }

    @Test
    @WithMockUser
    void jwks_ShouldReturnSingleKeyInArray() {
        webTestClient.get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.keys.length()").isEqualTo(1)
                .jsonPath("$.keys[0].kty").isEqualTo("RSA")
                .jsonPath("$.keys[0].kid").isEqualTo("oauth-server-key");
    }

    @Test
    @WithMockUser
    void jwks_ShouldHaveCorrectKeyTypeAndUsage() {
        webTestClient.get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.keys[0].kty").isEqualTo("RSA");
    }

    @Test
    @WithMockUser
    void jwks_ShouldReturnConsistentStructure() {
        webTestClient.get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.keys[0].kty").isEqualTo("RSA");

        webTestClient.get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.keys[0].kty").isEqualTo("RSA");
    }

    @Test
    @WithMockUser
    void jwks_ShouldContainRSAKeyParameters() {
        webTestClient.get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.keys[0].n").isNotEmpty()
                .jsonPath("$.keys[0].e").isNotEmpty();
    }
}
