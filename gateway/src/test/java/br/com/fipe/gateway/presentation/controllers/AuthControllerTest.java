package br.com.fipe.gateway.presentation.controllers;

import br.com.fipe.gateway.application.ports.input.JwtServicePort;
import br.com.fipe.gateway.application.ports.input.UserServicePort;
import br.com.fipe.gateway.config.TestSecurityConfig;
import br.com.fipe.gateway.domain.dto.User;
import br.com.fipe.gateway.presentation.dto.request.LoginRequest;
import br.com.fipe.gateway.presentation.dto.response.TokenResponse;
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
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebFluxTest(AuthController.class)
@ExtendWith(MockitoExtension.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserServicePort userServicePort;

    @MockBean
    private JwtServicePort jwtServicePort;

    private User testUser;
    private LoginRequest validLoginRequest;
    private LoginRequest invalidLoginRequest;
    private String testToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .password("encodedPassword")
                .roles(List.of("ROLE_USER", "ROLE_ADMIN"))
                .enabled(true)
                .build();

        validLoginRequest = new LoginRequest("testuser", "password123");
        invalidLoginRequest = new LoginRequest("wronguser", "wrongpassword");
        testToken = "test.jwt.token";
    }

    @Test
    @WithMockUser
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        Long expirationInSeconds = 3600L;

        when(userServicePort.authenticate(validLoginRequest.username(), validLoginRequest.password()))
                .thenReturn(Mono.just(testUser));
        when(jwtServicePort.generateToken(testUser)).thenReturn(testToken);
        when(jwtServicePort.getExpirationInSeconds()).thenReturn(expirationInSeconds);

        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validLoginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .value(response -> {
                    assert response.accessToken().equals(testToken);
                    assert response.tokenType().equals("Bearer");
                    assert response.expiresIn().equals(expirationInSeconds);
                });

        verify(userServicePort).authenticate(validLoginRequest.username(), validLoginRequest.password());
        verify(jwtServicePort).generateToken(testUser);
        verify(jwtServicePort).getExpirationInSeconds();
    }

    @Test
    @WithMockUser
    void login_ShouldReturnUnauthorized_WhenCredentialsAreInvalid() {
        when(userServicePort.authenticate(invalidLoginRequest.username(), invalidLoginRequest.password()))
                .thenReturn(Mono.error(new RuntimeException("Invalid credentials")));

        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidLoginRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(TokenResponse.class)
                .value(response -> {
                    assert response.accessToken() == null;
                    assert response.tokenType() == null;
                    assert response.expiresIn() == null;
                });

        verify(userServicePort).authenticate(invalidLoginRequest.username(), invalidLoginRequest.password());
    }

    @Test
    @WithMockUser
    void login_ShouldReturnUnauthorized_WhenUserNotFound() {
        when(userServicePort.authenticate("nonexistent", "password"))
                .thenReturn(Mono.error(new RuntimeException("User not found")));

        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("nonexistent", "password"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(TokenResponse.class)
                .value(response -> {
                    assert response.accessToken() == null;
                    assert response.tokenType() == null;
                    assert response.expiresIn() == null;
                });

        verify(userServicePort).authenticate("nonexistent", "password");
    }

    @Test
    @WithMockUser
    void login_ShouldReturnUnauthorized_WhenUserServiceThrowsException() {
        when(userServicePort.authenticate(anyString(), anyString()))
                .thenReturn(Mono.error(new IllegalStateException("Service unavailable")));

        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validLoginRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(TokenResponse.class)
                .value(response -> {
                    assert response.accessToken() == null;
                    assert response.tokenType() == null;
                    assert response.expiresIn() == null;
                });

        verify(userServicePort).authenticate(validLoginRequest.username(), validLoginRequest.password());
    }

    @Test
    @WithMockUser
    void login_ShouldHandleEmptyCredentials() {
        LoginRequest emptyCredentials = new LoginRequest("", "");

        when(userServicePort.authenticate("", ""))
                .thenReturn(Mono.error(new RuntimeException("Invalid credentials")));

        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(emptyCredentials)
                .exchange()
                .expectStatus().isUnauthorized();

        verify(userServicePort).authenticate("", "");
    }

    @Test
    @WithMockUser
    void login_ShouldReturnCorrectTokenStructure() {
        Long expirationInSeconds = 7200L;

        when(userServicePort.authenticate(validLoginRequest.username(), validLoginRequest.password()))
                .thenReturn(Mono.just(testUser));
        when(jwtServicePort.generateToken(testUser)).thenReturn(testToken);
        when(jwtServicePort.getExpirationInSeconds()).thenReturn(expirationInSeconds);

        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validLoginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo(testToken)
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.expiresIn").isEqualTo(expirationInSeconds);

        verify(userServicePort).authenticate(validLoginRequest.username(), validLoginRequest.password());
        verify(jwtServicePort).generateToken(testUser);
        verify(jwtServicePort).getExpirationInSeconds();
    }

    @Test
    void login_WithoutAuthentication_ShouldWork() {
        when(userServicePort.authenticate(validLoginRequest.username(), validLoginRequest.password()))
                .thenReturn(Mono.just(testUser));
        when(jwtServicePort.generateToken(testUser)).thenReturn(testToken);
        when(jwtServicePort.getExpirationInSeconds()).thenReturn(3600L);

        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validLoginRequest)
                .exchange()
                .expectStatus().isOk();

        verify(userServicePort).authenticate(validLoginRequest.username(), validLoginRequest.password());
    }

    @Test
    @WithMockUser
    void login_ShouldReturnUnauthorized_WhenJwtServiceException() {
        when(userServicePort.authenticate(validLoginRequest.username(), validLoginRequest.password()))
                .thenReturn(Mono.just(testUser));
        when(jwtServicePort.generateToken(testUser)).thenThrow(new RuntimeException("JWT generation failed"));

        webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validLoginRequest)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(TokenResponse.class)
                .value(response -> {
                    assert response.accessToken() == null;
                    assert response.tokenType() == null;
                    assert response.expiresIn() == null;
                });

        verify(userServicePort).authenticate(validLoginRequest.username(), validLoginRequest.password());
        verify(jwtServicePort).generateToken(testUser);
    }
}
