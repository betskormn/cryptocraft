package com.cryptocraft.apigateway.config;

import com.cryptocraft.apigateway.security.IdentityLogoutSuccessHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.server.DefaultServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.WebSessionServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * The gateway is the "backend for frontend" of the admin UI and the kitchen/bar screens: it signs staff in at
 * identity-service (authorization code + PKCE, as a confidential client), keeps their tokens in its session and
 * adds the access token to API calls (SessionTokenRelayFilter). The browser holds only an HttpOnly session
 * cookie, so page scripts never see a token.
 * <p>
 * The gateway itself lets every request through: each service decides with the token what the caller may do.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class SecurityConfig {

    private static final String SESSION_COOKIE = "SESSION";
    private static final Set<HttpMethod> SAFE_METHODS = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         ReactiveClientRegistrationRepository clientRegistrations,
                                                         GatewaySecurityProperties properties) {
        DefaultServerOAuth2AuthorizationRequestResolver authorizationRequests =
                new DefaultServerOAuth2AuthorizationRequestResolver(clientRegistrations);
        authorizationRequests.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        http
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                // GET /oauth2/authorization/identity starts signing in; identity-service redirects back to /login/oauth2/code/identity
                .oauth2Login(login -> login
                        .authorizationRequestResolver(authorizationRequests)
                        .authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler(properties.publicUrl() + "/"))
                        .authenticationFailureHandler(new RedirectServerAuthenticationFailureHandler(properties.publicUrl() + "/?signin=failed")))
                // POST /logout ends this session and the one at identity-service
                .logout(logout -> logout.logoutSuccessHandler(new IdentityLogoutSuccessHandler(properties)))
                .csrf(csrf -> csrf
                        // the UI reads XSRF-TOKEN and sends it back as X-XSRF-TOKEN (axios does it by itself)
                        .csrfTokenRepository(CookieServerCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new ServerCsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(cookieAuthenticatedChanges()));
        return http.build();
    }

    /**
     * CSRF only matters where the browser's session cookie is what authenticates a change. A guest or a TV
     * without a session, and a client sending its own bearer token, are not exposed to it.
     */
    private static ServerWebExchangeMatcher cookieAuthenticatedChanges() {
        return exchange -> {
            var request = exchange.getRequest();
            boolean change = !SAFE_METHODS.contains(request.getMethod());
            boolean session = request.getCookies().containsKey(SESSION_COOKIE);
            boolean bearer = request.getHeaders().containsHeader("Authorization");
            return change && session && !bearer ? ServerWebExchangeMatcher.MatchResult.match() : ServerWebExchangeMatcher.MatchResult.notMatch();
        };
    }

    // the CSRF cookie is written only once its token is asked for
    @Bean
    public WebFilter csrfCookieWebFilter() {
        return (exchange, chain) -> {
            Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
            return (token != null ? token.then() : Mono.<Void>empty()).then(chain.filter(exchange));
        };
    }

    // tokens live in the browser's session: gone when it signs out or the session expires
    @Bean
    public ServerOAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new WebSessionServerOAuth2AuthorizedClientRepository();
    }

    // refreshes an expired access token with the refresh token before relaying it
    @Bean
    public ReactiveOAuth2AuthorizedClientManager authorizedClientManager(ReactiveClientRegistrationRepository clientRegistrations,
                                                                         ServerOAuth2AuthorizedClientRepository authorizedClients) {
        DefaultReactiveOAuth2AuthorizedClientManager manager =
                new DefaultReactiveOAuth2AuthorizedClientManager(clientRegistrations, authorizedClients);
        manager.setAuthorizedClientProvider(ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build());
        return manager;
    }
}
