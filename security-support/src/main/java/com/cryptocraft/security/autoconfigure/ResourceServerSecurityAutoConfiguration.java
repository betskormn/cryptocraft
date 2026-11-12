package com.cryptocraft.security.autoconfigure;

import com.cryptocraft.security.ServiceSecurityRules;
import com.cryptocraft.security.web.ProblemDetailSecurityHandlers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * A service that declares {@link ServiceSecurityRules} becomes an OAuth2 resource server: stateless, every
 * request but the open paths needs a JWT signed by identity-service, then the service's rules decide.
 */
// before Boot's default chains, which back off once this one exists
@AutoConfiguration(beforeName = {
        "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
        "org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration",
        "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration",
        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration"
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(ServiceSecurityRules.class)
@EnableConfigurationProperties(SecuritySupportProperties.class)
public class ResourceServerSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(SecuritySupportProperties properties) throws Exception {
        if (properties.publicKey() != null && !properties.publicKey().isBlank()) {
            byte[] der = Base64.getDecoder().decode(properties.publicKey().trim());
            RSAPublicKey key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
            return NimbusJwtDecoder.withPublicKey(key).build();
        }
        if (properties.jwkSetUri() == null || properties.jwkSetUri().isBlank()) {
            throw new IllegalStateException("cryptocraft.security.jwk-set-uri is not set");
        }
        // keys are fetched on the first request and cached, so identity-service may start later
        return NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http,
                                                         SecuritySupportProperties properties,
                                                         ObjectProvider<ServiceSecurityRules> rules) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(properties.openPaths().toArray(String[]::new)).permitAll();
                    rules.orderedStream().forEach(r -> r.configure(authorize));
                    authorize.anyRequest().authenticated();
                })
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> {
                        })
                        .authenticationEntryPoint(ProblemDetailSecurityHandlers.unauthorized())
                        .accessDeniedHandler(ProblemDetailSecurityHandlers.forbidden()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(ProblemDetailSecurityHandlers.unauthorized())
                        .accessDeniedHandler(ProblemDetailSecurityHandlers.forbidden()));
        return http.build();
    }
}
