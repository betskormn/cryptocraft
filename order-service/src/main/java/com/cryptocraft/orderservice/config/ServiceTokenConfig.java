package com.cryptocraft.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;

/**
 * Order Service's own token for calls to Inventory: client credentials from identity-service, cached until
 * it expires. It does not depend on who called Order (a guest, staff, the retry job), so it works on
 * background threads too.
 */
@Configuration
public class ServiceTokenConfig {

    // spring.security.oauth2.client.registration.identity
    public static final String REGISTRATION_ID = "identity";

    // one token for the whole service, whatever the current request's user
    private static final Authentication SERVICE = new AnonymousAuthenticationToken(
            "order-service", "order-service", AuthorityUtils.createAuthorityList("ROLE_SERVICE"));

    @Bean
    public OAuth2AuthorizedClientManager serviceTokenManager(ClientRegistrationRepository clientRegistrations,
                                                             OAuth2AuthorizedClientService authorizedClients) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrations, authorizedClients);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return manager;
    }

    @Bean
    public OAuth2ClientHttpRequestInterceptor serviceTokenInterceptor(OAuth2AuthorizedClientManager serviceTokenManager) {
        OAuth2ClientHttpRequestInterceptor interceptor = new OAuth2ClientHttpRequestInterceptor(serviceTokenManager);
        interceptor.setClientRegistrationIdResolver(request -> REGISTRATION_ID);
        interceptor.setPrincipalResolver(request -> SERVICE);
        return interceptor;
    }
}
