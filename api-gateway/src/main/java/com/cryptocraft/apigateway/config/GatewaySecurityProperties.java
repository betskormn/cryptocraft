package com.cryptocraft.apigateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param publicUrl          where the admin UI is opened (http://localhost:5173); staff land here after signing in or out
 * @param identityPublicUrl  identity-service as browsers reach it, for signing out there too
 */
@ConfigurationProperties("cryptocraft.gateway")
public record GatewaySecurityProperties(String publicUrl, String identityPublicUrl) {
}
