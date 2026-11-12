package com.cryptocraft.identityservice.config;

import com.cryptocraft.security.CryptoCraftClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The applications allowed to get tokens come from configuration (secrets from the environment) and are
 * written to the database on every start, so a changed secret or redirect URI applies after a restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisteredClientsInitializer implements ApplicationRunner {

    private final IdentityProperties properties;
    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        IdentityProperties.GatewayClient gateway = properties.gatewayClient();
        registeredClientRepository.save(RegisteredClient.withId(idOf(gateway.clientId()))
                .clientId(gateway.clientId())
                .clientSecret(passwordEncoder.encode(gateway.secret()))
                .clientName("CryptoCraft gateway (admin, kitchen and bar screens)")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(gateway.redirectUri())
                .postLogoutRedirectUri(gateway.postLogoutRedirectUri())
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .clientSettings(ClientSettings.builder()
                        // our own application: nothing to consent to
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(properties.accessTokenTtl())
                        .refreshTokenTimeToLive(properties.refreshTokenTtl())
                        // a new refresh token on every refresh: a stolen one stops working once used
                        .reuseRefreshTokens(false)
                        .build())
                .build());

        properties.serviceClients().forEach(service -> registeredClientRepository.save(RegisteredClient.withId(idOf(service.clientId()))
                .clientId(service.clientId())
                .clientSecret(passwordEncoder.encode(service.secret()))
                .clientName(service.clientId())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(CryptoCraftClaims.SCOPE_INTERNAL)
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(properties.accessTokenTtl()).build())
                .build()));

        log.info("Registered OAuth2 clients: {} and {} service client(s)", gateway.clientId(), properties.serviceClients().size());
    }

    // keep the row's id, so saving updates it
    private String idOf(String clientId) {
        RegisteredClient existing = registeredClientRepository.findByClientId(clientId);
        return existing != null ? existing.getId() : UUID.randomUUID().toString();
    }
}
