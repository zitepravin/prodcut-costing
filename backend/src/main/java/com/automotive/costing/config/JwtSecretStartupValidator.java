package com.automotive.costing.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fails fast when JWT_SECRET is still the documented placeholder outside local/dev profiles.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtSecretStartupValidator implements ApplicationRunner {

    static final String PLACEHOLDER = "CHANGE_ME_IN_PRODUCTION_MIN_256_BIT_SECRET_VALUE_0001";

    private final JwtProperties jwtProperties;
    private final SecurityProperties securityProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (!securityProperties.isRequireStrongJwtSecret()) {
            log.warn("JWT secret strength check disabled (app.security.require-strong-jwt-secret=false)");
            return;
        }
        for (String p : environment.getActiveProfiles()) {
            if ("local".equalsIgnoreCase(p) || "dev".equalsIgnoreCase(p) || "test".equalsIgnoreCase(p)) {
                log.info("Profile '{}' active — allowing placeholder JWT secret for local use", p);
                return;
            }
        }
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank() || PLACEHOLDER.equals(secret) || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET is missing, too short (<32 chars), or still the default placeholder. "
                            + "Set a strong secret via JWT_SECRET before starting in non-local environments.");
        }
    }
}
