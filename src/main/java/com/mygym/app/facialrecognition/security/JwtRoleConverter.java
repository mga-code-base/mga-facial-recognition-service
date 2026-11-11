package com.mygym.app.facialrecognition.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

public final class JwtRoleConverter {

    public static final String ROLE_CLAIM = "role";

    private JwtRoleConverter() {
    }

    public static JwtAuthenticationConverter create() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString(ROLE_CLAIM);
            return role == null ? List.of() : List.of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }
}
