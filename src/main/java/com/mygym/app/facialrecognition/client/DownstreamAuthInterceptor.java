package com.mygym.app.facialrecognition.client;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class DownstreamAuthInterceptor implements ClientHttpRequestInterceptor {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final String BEARER = "Bearer ";

    private final ServiceTokenProvider serviceTokens;

    public DownstreamAuthInterceptor(ServiceTokenProvider serviceTokens) {
        this.serviceTokens = serviceTokens;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        HttpHeaders headers = request.getHeaders();
        Optional<HttpServletRequest> incoming = currentRequest();
        String userToken = incoming.map(r -> r.getHeader(HttpHeaders.AUTHORIZATION))
                .filter(value -> value.startsWith(BEARER))
                .orElse(null);
        headers.set(HttpHeaders.AUTHORIZATION, userToken != null ? userToken : BEARER + serviceTokens.token());
        incoming.map(r -> r.getHeader(IDEMPOTENCY_KEY))
                .filter(key -> !key.isBlank())
                .ifPresent(key -> headers.set(IDEMPOTENCY_KEY, key));
        return execution.execute(request, body);
    }

    private Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return Optional.of(attributes.getRequest());
        }
        return Optional.empty();
    }
}
