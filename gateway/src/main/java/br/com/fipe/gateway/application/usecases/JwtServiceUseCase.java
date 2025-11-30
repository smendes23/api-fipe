package br.com.fipe.gateway.application.usecases;

import br.com.fipe.gateway.application.ports.JwtServicePort;
import br.com.fipe.gateway.domain.dto.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtServiceUseCase implements JwtServicePort {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration, ChronoUnit.MILLIS);

        String scope = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getUsername())
                .claim("scope", scope)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public Jwt validateToken(String token) {
        return jwtDecoder.decode(token);
    }

    public Long getExpirationInSeconds() {
        return expiration / 1000;
    }
}
