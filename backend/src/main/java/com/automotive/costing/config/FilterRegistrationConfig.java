package com.automotive.costing.config;

import com.automotive.costing.platform.access.CustomerContextFilter;
import com.automotive.costing.security.ApiRateLimitFilter;
import com.automotive.costing.security.IdempotencyKeyFilter;
import com.automotive.costing.security.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prevent double registration of security-related OncePerRequestFilters
 * that are already installed in {@link SecurityConfig}'s filter chain.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<ApiRateLimitFilter> apiRateLimitRegistration(ApiRateLimitFilter filter) {
        FilterRegistrationBean<ApiRateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<CustomerContextFilter> customerContextRegistration(CustomerContextFilter filter) {
        FilterRegistrationBean<CustomerContextFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<IdempotencyKeyFilter> idempotencyRegistration(IdempotencyKeyFilter filter) {
        FilterRegistrationBean<IdempotencyKeyFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }
}
