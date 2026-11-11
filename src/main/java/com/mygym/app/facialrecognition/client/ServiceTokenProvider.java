package com.mygym.app.facialrecognition.client;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.mygym.app.facialrecognition.security.JwtRoleConverter;

@Component
public class ServiceTokenProvider {

    public static final String SERVICE_ROLE = "SERVICE";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);
    private static final Duration REFRESH_MARGIN = Duration.ofMinutes(1);

    private final JwtEncoder encoder;
    private final String serviceName;
    private final Clock clock;
    private final AtomicReference<IssuedToken> current = new AtomicReference<>();

    public ServiceTokenProvider(JwtEncoder encoder, @Value("${spring.application.name}") String serviceName) {
        this(encoder, serviceName, Clock.systemUTC());
    }

    ServiceTokenProvider(JwtEncoder encoder, String serviceName, Clock clock) {
        this.encoder = encoder;
        this.serviceName = serviceName;
        this.clock = clock;
    }

    public String token() {
        Instant now = clock.instant();
        IssuedToken token = current.get();
        if (token == null || now.isAfter(token.expiresAt().minus(REFRESH_MARGIN))) {
            token = issue(now);
            current.set(token);
        }
        return token.value();
    }

    private IssuedToken issue(Instant now) {
        Instant expiresAt = now.plus(TOKEN_TTL);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(serviceName)
                .issuer(serviceName)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(JwtRoleConverter.ROLE_CLAIM, SERVICE_ROLE)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    private record IssuedToken(String value, Instant expiresAt) {
    }
}
