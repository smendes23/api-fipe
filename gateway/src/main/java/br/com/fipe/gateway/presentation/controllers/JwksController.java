package br.com.fipe.gateway.presentation.controllers;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Tag(name = "WELL-KNOWN", description = "Service validate jwt")
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RSAPublicKey publicKey;

    @GetMapping("/.well-known/jwks.json")
    public Mono<Map<String, Object>> jwks() {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .keyID("oauth-server-key")
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return Mono.just(jwkSet.toJSONObject());
    }
}
