package com.cryptocraft.authenticationservice.repository;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import java.util.HashMap;
import java.util.Map;

public class CustomClientRegistrationRepository implements ClientRegistrationRepository {

    private final Map<String, ClientRegistration> registrations = new HashMap<>();

    public CustomClientRegistrationRepository() {
        registrations.put("google", googleClientRegistration());
        registrations.put("apple", appleClientRegistration());
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return registrations.get(registrationId);
    }

    private ClientRegistration googleClientRegistration() {
        return ClientRegistration.withRegistrationId("google")
                .clientId("googleClientId")
                .clientSecret("googleClientSecret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://www.googleapis.com/oauth2/v3/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .build();
    }

    private ClientRegistration appleClientRegistration() {
        return ClientRegistration.withRegistrationId("apple")
                .clientId("appleClientId")
                .clientSecret("appleClientSecret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://appleid.apple.com/auth/authorize")
                .tokenUri("https://appleid.apple.com/auth/token")
                .userInfoUri("https://appleid.apple.com/auth/userinfo")
                .userNameAttributeName("sub")
                .clientName("Apple")
                .build();
    }
}
