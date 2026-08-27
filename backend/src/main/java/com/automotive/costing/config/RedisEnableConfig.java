package com.automotive.costing.config;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Imports Redis auto-configuration only when rate-limit-store=redis.
 */
@Configuration
@ConditionalOnProperty(name = "app.security.rate-limit-store", havingValue = "redis")
@ImportAutoConfiguration(RedisAutoConfiguration.class)
public class RedisEnableConfig {
}
