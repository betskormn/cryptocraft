package com.cryptocraft.identityservice.config;

import com.cryptocraft.identityservice.security.SigningKeyStore;
import com.cryptocraft.security.VenueAccess;
import com.cryptocraft.security.VenueRole;
import com.cryptocraft.security.web.ProblemDetailSecurityHandlers;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Three filter chains:
 * <ol>
 *     <li>OAuth2 / OpenID Connect endpoints of Spring Authorization Server (/oauth2/**, /.well-known/**, /connect/logout, /userinfo);</li>
 *     <li>this service's API (/api/**): stateless, staff tokens, like every other service;</li>
 *     <li>the sign-in page: the only place a password is typed. It keeps a session so the authorization
 *     endpoint knows who is signed in.</li>
 * </ol>
 */
@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {
        http
                .oauth2AuthorizationServer(authorizationServer -> {
                    http.securityMatcher(authorizationServer.getEndpointsMatcher());
                    authorizationServer.oidc(Customizer.withDefaults());
                })
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                // a browser that is not signed in is sent to the sign-in page
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                // /userinfo is called with an access token
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/me", "/api/v1/me/**").authenticated()
                        .requestMatchers("/api/v1/staff", "/api/v1/staff/**").access(VenueAccess.venueRole(VenueRole.ADMIN))
                        // Order Service, for a guest who joined a table
                        .requestMatchers(HttpMethod.POST, "/api/v1/guest-tokens").access(VenueAccess.service())
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(ProblemDetailSecurityHandlers.unauthorized())
                        .accessDeniedHandler(ProblemDetailSecurityHandlers.forbidden()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(ProblemDetailSecurityHandlers.unauthorized())
                        .accessDeniedHandler(ProblemDetailSecurityHandlers.forbidden()));
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain signInFilterChain(HttpSecurity http, IdentityProperties properties) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/css/**", "/error", "/actuator/health/**", "/actuator/info",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        // opened directly, without an application asking: go to the admin
                        .defaultSuccessUrl(properties.gatewayClient().postLogoutRedirectUri()));
        return http.build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(SigningKeyStore signingKeyStore) {
        return new ImmutableJWKSet<>(new JWKSet(signingKeyStore.currentKey()));
    }

    // our own tokens, for /api/** and /userinfo; checked with the public key, no HTTP call
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource, SigningKeyStore signingKeyStore) throws JOSEException {
        RSAKey key = signingKeyStore.currentKey();
        return NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).build();
    }

    // signs guest tokens with the same key (Spring Authorization Server uses it too)
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings(IdentityProperties properties) {
        return AuthorizationServerSettings.builder().issuer(properties.issuer()).build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    // signed-in sessions survive a restart: authorizations (with their refresh tokens) are in the database
    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, clients);
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, clients);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
