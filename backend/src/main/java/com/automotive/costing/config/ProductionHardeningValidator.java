package com.automotive.costing.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Fail-fast production guards: no memory rate limit, no weak JWT, required secrets.
 */
@Component
public class ProductionHardeningValidator {

    private final Environment environment;

    @Value("${app.security.rate-limit-store:memory}")
    private String rateLimitStore;

    @Value("${app.security.require-strong-jwt-secret:true}")
    private boolean requireStrongJwt;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    public ProductionHardeningValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validate() {
        boolean prod = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
        if (!prod) {
            return;
        }
        if (!"redis".equalsIgnoreCase(rateLimitStore)) {
            throw new IllegalStateException(
                    "PRODUCTION BLOCKER: app.security.rate-limit-store must be 'redis' under profile prod. "
                            + "Memory store is not multi-instance safe.");
        }
        if (requireStrongJwt) {
            if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32
                    || jwtSecret.contains("changeme") || jwtSecret.contains("secret")
                    || jwtSecret.equalsIgnoreCase("local-dev-only-not-for-production")) {
                throw new IllegalStateException(
                        "PRODUCTION BLOCKER: JWT_SECRET must be a strong secret (>=32 chars, non-placeholder).");
            }
        }
        String dbPass = environment.getProperty("spring.datasource.password", "");
        if (dbPass.isBlank() || "changeme".equals(dbPass) || "rootpass".equals(dbPass)) {
            throw new IllegalStateException(
                    "PRODUCTION BLOCKER: DB_PASSWORD must be set to a non-default value.");
        }
        String redisHost = environment.getProperty("spring.data.redis.host", "");
        if (redisHost.isBlank()) {
            throw new IllegalStateException(
                    "PRODUCTION BLOCKER: REDIS_HOST (spring.data.redis.host) is required when rate-limit-store=redis.");
        }
        String cors = environment.getProperty("app.security.cors-allowed-origins", "");
        if (cors.isBlank() || cors.contains("localhost")) {
            // warn-level only for localhost residual — still fail if blank
            if (cors.isBlank()) {
                throw new IllegalStateException(
                        "PRODUCTION BLOCKER: CORS_ORIGINS must be set explicitly.");
            }
        }
    }
}
