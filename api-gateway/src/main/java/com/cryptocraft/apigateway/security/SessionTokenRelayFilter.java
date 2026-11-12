package com.cryptocraft.apigateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * A signed-in browser's API calls carry its access token, refreshed when it has expired. When it cannot be
 * refreshed (the sign-in expired or the account was disabled) the session is ended and the call goes on
 * without a token, so the service answers 401 and the UI signs in again.
 * <p>
 * Without a session, or with its own bearer token (a guest's page), the request is passed as it is.
 */
@Component
public class SessionTokenRelayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(SessionTokenRelayFilter.class);

    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    public SessionTokenRelayFilter(ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // a token the caller sent itself wins: a guest's page opened in a browser that also has a staff session
        if (exchange.getRequest().getHeaders().containsHeader(HttpHeaders.AUTHORIZATION)) {
            return chain.filter(exchange);
        }
        return exchange.getPrincipal()
                .filter(OAuth2AuthenticationToken.class::isInstance)
                .cast(OAuth2AuthenticationToken.class)
                .flatMap(principal -> authorizedClientManager.authorize(
                                OAuth2AuthorizeRequest.withClientRegistrationId(principal.getAuthorizedClientRegistrationId())
                                        .principal(principal)
                                        .attribute(ServerWebExchange.class.getName(), exchange)
                                        .build())
                        .onErrorResume(e -> {
                            log.info("Signed-in session dropped, tokens could not be refreshed: {}", e.getMessage());
                            return exchange.getSession().flatMap(session -> session.invalidate()).then(Mono.empty());
                        }))
                .map(client -> withToken(exchange, client))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private static ServerWebExchange withToken(ServerWebExchange exchange, OAuth2AuthorizedClient client) {
        return exchange.mutate()
                .request(request -> request.headers(headers -> headers.setBearerAuth(client.getAccessToken().getTokenValue())))
                .build();
    }

    // before the request is routed
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
