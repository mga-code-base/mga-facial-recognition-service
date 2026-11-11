package com.mygym.app.facialrecognition.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;

import com.mygym.app.facialrecognition.web.DownstreamUnavailableException;

public class ResilienceInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResilienceInterceptor.class);
    private static final Set<HttpMethod> RETRYABLE = Set.of(HttpMethod.GET, HttpMethod.HEAD);

    private final String downstream;
    private final CircuitBreaker circuitBreaker;
    private final int maxAttempts;
    private final Duration backoff;

    public ResilienceInterceptor(String downstream, CircuitBreaker circuitBreaker, int maxAttempts, Duration backoff) {
        this.downstream = downstream;
        this.circuitBreaker = circuitBreaker;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.backoff = backoff;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
        return circuitBreaker.run(() -> executeWithRetry(request, body, execution), this::fallback);
    }

    private ClientHttpResponse executeWithRetry(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
        int attempts = RETRYABLE.contains(request.getMethod()) ? maxAttempts : 1;
        RuntimeException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                if (!response.getStatusCode().is5xxServerError()) {
                    return response;
                }
                last = toServerError(response);
            } catch (IOException e) {
                last = new UncheckedIOException(e);
            }
            if (attempt < attempts) {
                LOGGER.debug("Retrying {} {} on {} (attempt {})", request.getMethod(), request.getURI().getPath(),
                        downstream, attempt + 1);
                pause(attempt);
            }
        }
        throw last;
    }

    private ClientHttpResponse fallback(Throwable failure) {
        if (failure instanceof RestClientResponseException responseException) {
            throw responseException;
        }
        throw new DownstreamUnavailableException(downstream, failure);
    }

    private HttpServerErrorException toServerError(ClientHttpResponse response) throws IOException {
        try (response) {
            byte[] payload = StreamUtils.copyToByteArray(response.getBody());
            return HttpServerErrorException.create(response.getStatusCode(), response.getStatusText(),
                    response.getHeaders(), payload, StandardCharsets.UTF_8);
        }
    }

    private void pause(int attempt) {
        try {
            Thread.sleep(backoff.multipliedBy(attempt).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying " + downstream, e);
        }
    }
}
