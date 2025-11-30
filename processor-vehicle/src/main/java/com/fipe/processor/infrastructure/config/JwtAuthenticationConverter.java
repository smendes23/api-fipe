package com.fipe.processor.infrastructure.config;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        return Mono.just(createAuthenticationToken(jwt));
    }

    private AbstractAuthenticationToken createAuthenticationToken(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");

        if (scope == null || scope.isEmpty()) {
            return Collections.emptyList();
        }

        return Stream.of(scope.split(" "))
                .filter(role -> !role.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}