package br.com.fipe.gateway.presentation.controllers;

import br.com.fipe.gateway.application.ports.JwtServicePort;
import br.com.fipe.gateway.application.ports.UserServicePort;
import br.com.fipe.gateway.presentation.dto.request.LoginRequest;
import br.com.fipe.gateway.presentation.dto.response.TokenResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "AUTH", description = "Service authentication")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserServicePort userService;
    private final JwtServicePort jwtService;

    @PostMapping("/login")
    public Mono<ResponseEntity<TokenResponse>> login(@RequestBody LoginRequest loginRequest) {
        return userService.authenticate(loginRequest.username(), loginRequest.password())
                .map(user -> {
                    String token = jwtService.generateToken(user);
                    TokenResponse response = new TokenResponse(token ,"Bearer", jwtService.getExpirationInSeconds());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new TokenResponse(null,null,null))
                ));
    }
}
