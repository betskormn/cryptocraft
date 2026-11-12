package com.cryptocraft.apigateway.security;

import com.cryptocraft.apigateway.config.GatewaySecurityProperties;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.DefaultServerRedirectStrategy;
import org.springframework.security.web.server.ServerRedirectStrategy;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * After the gateway's session is gone, signs out at identity-service too (OpenID Connect RP-initiated
 * logout), so the next sign-in asks for the password again; identity-service then sends the browser back
 * to the admin UI.
 */
public class IdentityLogoutSuccessHandler implements ServerLogoutSuccessHandler {

    private final GatewaySecurityProperties properties;
    private final ServerRedirectStrategy redirectStrategy = new DefaultServerRedirectStrategy();

    public IdentityLogoutSuccessHandler(GatewaySecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, Authentication authentication) {
        String home = properties.publicUrl() + "/";
        URI target = authentication != null && authentication.getPrincipal() instanceof OidcUser user
                ? UriComponentsBuilder.fromUriString(properties.identityPublicUrl() + "/connect/logout")
                .queryParam("id_token_hint", user.getIdToken().getTokenValue())
                .queryParam("post_logout_redirect_uri", home)
                .build()
                .toUri()
                : URI.create(home);
        return redirectStrategy.sendRedirect(exchange.getExchange(), target);
    }
}
