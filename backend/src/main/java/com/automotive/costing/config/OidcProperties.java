package com.automotive.costing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SEC-02 SSO (OIDC) configuration scaffold. Disabled by default.
 * When enabled on a deployment that adds spring-boot-starter-oauth2-client /
 * oauth2-resource-server, wire issuer-uri and client registration externally.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security.oidc")
public class OidcProperties {

    /** Master switch — must stay false until OAuth2 client dependencies and IdP are configured. */
    private boolean enabled = false;

    private String issuerUri = "";
    private String clientId = "";
    private String clientSecret = "";
}
