package br.com.fipe.gateway.application.ports.input;

import br.com.fipe.gateway.domain.dto.User;
import org.springframework.security.oauth2.jwt.Jwt;

public interface JwtServicePort {

    String generateToken(User user);
    Jwt validateToken(String token);
    Long getExpirationInSeconds();
}
