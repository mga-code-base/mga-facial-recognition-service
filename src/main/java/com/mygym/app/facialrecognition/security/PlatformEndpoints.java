package com.mygym.app.facialrecognition.security;

public final class PlatformEndpoints {

    public static final String[] PUBLIC = {
            "/actuator/health", "/actuator/health/**", "/actuator/info", "/actuator/prometheus",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/error"
    };

    private PlatformEndpoints() {
    }
}
