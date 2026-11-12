package com.cryptocraft.security.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @param jwkSetUri  identity-service's public keys (http://identity-service:8086/oauth2/jwks)
 * @param publicKey  a fixed RSA public key (Base64 X.509) instead of the key set; for tests
 * @param openPaths  path patterns served without a token (health, API docs, service-to-service)
 */
@ConfigurationProperties("cryptocraft.security")
public record SecuritySupportProperties(String jwkSetUri, String publicKey, List<String> openPaths) {

    public SecuritySupportProperties {
        if (openPaths == null) {
            openPaths = List.of("/internal/**", "/actuator/health/**", "/actuator/info", "/v3/api-docs/**",
                    "/swagger-ui/**", "/swagger-ui.html", "/error");
        }
    }
}
