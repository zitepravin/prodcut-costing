package com.automotive.costing.config;

import com.automotive.costing.security.JwtAuthenticationFilter;
import com.automotive.costing.security.ApiRateLimitFilter;
import com.automotive.costing.security.IdempotencyKeyFilter;
import com.automotive.costing.platform.access.CustomerContextFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomerContextFilter customerContextFilter;
    private final ApiRateLimitFilter apiRateLimitFilter;
    private final IdempotencyKeyFilter idempotencyKeyFilter;
    private final UserDetailsService userDetailsService;
    private final SecurityProperties securityProperties;
    private final Environment environment;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll();
                    auth.requestMatchers("/api/v1/portal/login", "/api/v1/portal/accept-invite").permitAll();
                    auth.requestMatchers("/api/v1/customer/invitations/accept").permitAll();
                    // Portal quote submit uses portal JWT validated in service (not internal UserDetails)
                    auth.requestMatchers("/api/v1/portal/quotes", "/api/v1/portal/my-rfqs").permitAll();
                    auth.requestMatchers("/api/v1/billing/webhooks/**").permitAll();
                    auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll();
                    if (isSwaggerPublic()) {
                        auth.requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(customerContextFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(apiRateLimitFilter, CustomerContextFilter.class)
                .addFilterAfter(idempotencyKeyFilter, ApiRateLimitFilter.class);

        return http.build();
    }

    private boolean isSwaggerPublic() {
        if (securityProperties.isSwaggerPublic()) {
            return true;
        }
        for (String p : environment.getActiveProfiles()) {
            if ("local".equalsIgnoreCase(p) || "dev".equalsIgnoreCase(p)) {
                return true;
            }
        }
        return false;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = securityProperties.resolvedCorsOrigins();
        if (origins == null || origins.isEmpty() || origins.stream().anyMatch(o -> "*".equals(o))) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(origins);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
