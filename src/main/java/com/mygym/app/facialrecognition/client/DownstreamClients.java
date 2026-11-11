package com.mygym.app.facialrecognition.client;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DownstreamClients {

    private final RestClient.Builder builder;
    private final CircuitBreakerFactory<?, ?> circuitBreakers;
    private final DownstreamAuthInterceptor authInterceptor;
    private final int maxAttempts;
    private final Duration backoff;

    public DownstreamClients(RestClient.Builder builder, CircuitBreakerFactory<?, ?> circuitBreakers,
            DownstreamAuthInterceptor authInterceptor,
            @Value("${mga.clients.retry.max-attempts:3}") int maxAttempts,
            @Value("${mga.clients.retry.backoff:200ms}") Duration backoff) {
        this.builder = builder;
        this.circuitBreakers = circuitBreakers;
        this.authInterceptor = authInterceptor;
        this.maxAttempts = maxAttempts;
        this.backoff = backoff;
    }

    public RestClient create(String downstream, String baseUrl) {
        return builder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(authInterceptor)
                .requestInterceptor(new ResilienceInterceptor(downstream, circuitBreakers.create(downstream), maxAttempts, backoff))
                .build();
    }
}
