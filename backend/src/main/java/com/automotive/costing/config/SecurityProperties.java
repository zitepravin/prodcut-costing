package com.automotive.costing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /** Comma-separated origins or "*" (also settable as YAML list). */
    private String corsAllowedOrigins = "*";

    private boolean requireStrongJwtSecret = true;
    private int loginRateLimitMaxAttempts = 20;
    private int loginRateLimitWindowSeconds = 300;
    private boolean swaggerPublic = false;

    public List<String> resolvedCorsOrigins() {
        if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank()) {
            return List.of("*");
        }
        return Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
