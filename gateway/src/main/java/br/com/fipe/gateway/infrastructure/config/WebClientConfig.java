package br.com.fipe.gateway.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient fipeWebClient(WebClient.Builder webClientBuilder,
                                   @Value("${processor.brand.base-url}") String baseUrl) {
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(authenticationFilter())
                .build();
    }

    private ExchangeFilterFunction authenticationFilter() {
        return (request, next) ->
                ReactiveSecurityContextHolder.getContext()
                        .map(securityContext -> {
                            Authentication authentication = securityContext.getAuthentication();
                            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                                Jwt jwt = (Jwt) authentication.getPrincipal();
                                String tokenValue = jwt.getTokenValue();
                                return ClientRequest.from(request)
                                        .header("Authorization", "Bearer " + tokenValue)
                                        .build();
                            }
                            return request;
                        })
                        .defaultIfEmpty(request)
                        .flatMap(next::exchange);
    }

}
