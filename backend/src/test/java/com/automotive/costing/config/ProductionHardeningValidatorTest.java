package com.automotive.costing.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionHardeningValidatorTest {

    @Test
    void nonProd_allowsMemoryStore() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        ProductionHardeningValidator v = new ProductionHardeningValidator(env);
        assertThatCode(v::validate).doesNotThrowAnyException();
    }

    @Test
    void prod_rejectsMemoryRateLimit() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("spring.datasource.password", "strong-db-password-xyz");
        env.setProperty("spring.data.redis.host", "redis");
        env.setProperty("app.security.cors-allowed-origins", "https://app.example.com");
        ProductionHardeningValidator v = new ProductionHardeningValidator(env);
        var store = ProductionHardeningValidator.class.getDeclaredField("rateLimitStore");
        store.setAccessible(true);
        store.set(v, "memory");
        var jwt = ProductionHardeningValidator.class.getDeclaredField("jwtSecret");
        jwt.setAccessible(true);
        jwt.set(v, "this-is-a-strong-jwt-secret-32chars!!");
        var req = ProductionHardeningValidator.class.getDeclaredField("requireStrongJwt");
        req.setAccessible(true);
        req.set(v, true);
        assertThatThrownBy(v::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("redis");
    }

    @Test
    void prod_rejectsWeakJwt() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("spring.datasource.password", "strong-db-password-xyz");
        env.setProperty("spring.data.redis.host", "redis");
        env.setProperty("app.security.cors-allowed-origins", "https://app.example.com");
        ProductionHardeningValidator v = new ProductionHardeningValidator(env);
        var store = ProductionHardeningValidator.class.getDeclaredField("rateLimitStore");
        store.setAccessible(true);
        store.set(v, "redis");
        var jwt = ProductionHardeningValidator.class.getDeclaredField("jwtSecret");
        jwt.setAccessible(true);
        jwt.set(v, "changeme");
        var req = ProductionHardeningValidator.class.getDeclaredField("requireStrongJwt");
        req.setAccessible(true);
        req.set(v, true);
        assertThatThrownBy(v::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT");
    }
}
