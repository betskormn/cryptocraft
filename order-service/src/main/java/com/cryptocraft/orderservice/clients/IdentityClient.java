package com.cryptocraft.orderservice.clients;

import com.cryptocraft.orderservice.clients.dto.GuestTokenDto;
import com.cryptocraft.orderservice.clients.dto.GuestTokenRequestDto;
import com.cryptocraft.orderservice.exceptions.DownstreamUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * identity-service signs the token of a guest who joined a table; asked with Order's own token.
 */
@Component
public class IdentityClient {

    // IllegalStateException in the catch: the load balancer's "no instances available" (service starting up);
    // OAuth2AuthorizationException: no token from identity-service
    private final RestClient restClient;

    public IdentityClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
                          OAuth2ClientHttpRequestInterceptor serviceTokenInterceptor,
                          @Value("${order.identity-url:http://identity-service}") String identityUrl) {
        this.restClient = loadBalancedRestClientBuilder.clone()
                .requestInterceptor(serviceTokenInterceptor)
                .baseUrl(identityUrl)
                .build();
    }

    public GuestTokenDto issueGuestToken(Long tenantId, UUID participantId, UUID sessionId) {
        try {
            GuestTokenDto token = restClient.post()
                    .uri("/api/v1/guest-tokens")
                    .body(new GuestTokenRequestDto(tenantId, participantId, sessionId))
                    .retrieve()
                    .body(GuestTokenDto.class);
            if (token == null) {
                throw new DownstreamUnavailableException("Guest sign-in is not available right now", null);
            }
            return token;
        } catch (RestClientException | IllegalStateException | OAuth2AuthorizationException e) {
            throw new DownstreamUnavailableException("Guest sign-in is not available right now", e);
        }
    }
}
