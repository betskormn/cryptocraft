package com.cryptocraft.identityservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * @param issuer           this server's public URL, as browsers reach it; it is the {@code iss} of every token
 * @param accessTokenTtl   lifetime of an access token; role changes reach a session at the next refresh
 * @param refreshTokenTtl  how long a signed-in session lasts without activity (a shift)
 * @param guestTokenTtl    a guest's token; longer than any visit, closing the visit ends it earlier
 * @param gatewayClient    the gateway signs staff in (authorization code) and keeps their tokens
 * @param serviceClients   our services calling each other (client credentials, scope "internal")
 * @param bootstrapAdmin   platform admin created on the first start, when there is none
 */
@ConfigurationProperties("identity")
public record IdentityProperties(
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration guestTokenTtl,
        GatewayClient gatewayClient,
        List<ServiceClient> serviceClients,
        BootstrapAdmin bootstrapAdmin
) {

    public IdentityProperties {
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(10);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofHours(12);
        }
        if (guestTokenTtl == null) {
            guestTokenTtl = Duration.ofHours(12);
        }
        if (serviceClients == null) {
            serviceClients = List.of();
        }
    }

    public record GatewayClient(String clientId, String secret, String redirectUri, String postLogoutRedirectUri) {
    }

    public record ServiceClient(String clientId, String secret) {
    }

    public record BootstrapAdmin(String email, String password, String displayName) {
    }
}
