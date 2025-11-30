package com.fipe.processor.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Configuration
public class WebClientConfig {
    private static final String USER_AGENT = "MyApp/1.0 (+http://myapp.com)";

    @Bean
    public WebClient fipeWebClient(WebClient.Builder webClientBuilder,
                                   @Value("${fipe.api.base-url}") String baseUrl) {
        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(ExchangeFilterFunction.ofRequestProcessor(Mono::just
                ))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}
