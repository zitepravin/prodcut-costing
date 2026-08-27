package com.automotive.costing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Product Costing Software backend.
 *
 * Architecture: Controller -> Service -> Repository -> Database.
 * Redis auto-config is excluded by default so local/dev without Redis still starts.
 * Enable with {@code app.security.rate-limit-store=redis} via {@code RedisEnableConfig}.
 */
@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
public class ProductCostingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductCostingApplication.class, args);
    }
}
