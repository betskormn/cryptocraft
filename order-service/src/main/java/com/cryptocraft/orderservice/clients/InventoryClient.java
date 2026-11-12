package com.cryptocraft.orderservice.clients;

import com.cryptocraft.orderservice.clients.dto.SellableItemDto;
import com.cryptocraft.orderservice.clients.dto.StockConsumptionRequestDto;
import com.cryptocraft.orderservice.clients.dto.TenantDto;
import com.cryptocraft.orderservice.exceptions.DownstreamUnavailableException;
import com.cryptocraft.orderservice.exceptions.StockWriteOffRejectedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Component
public class InventoryClient {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    // IllegalStateException in the catches: the load balancer's "no instances available" (service starting up);
    // OAuth2AuthorizationException: no token from identity-service

    private final RestClient restClient;

    // every call carries Order's own token (client credentials, ServiceTokenConfig)
    public InventoryClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
                           OAuth2ClientHttpRequestInterceptor serviceTokenInterceptor,
                           @Value("${order.inventory-url:http://inventory-service}") String inventoryUrl) {
        this.restClient = loadBalancedRestClientBuilder.clone()
                .requestInterceptor(serviceTokenInterceptor)
                .baseUrl(inventoryUrl)
                .build();
    }

    // the given items of the venue's menu with stop-list state; unknown ids are left out
    public List<SellableItemDto> getSellableItems(Long tenantId, Collection<Long> itemIds) {
        try {
            return restClient.get()
                    .uri(uri -> uri.path("/api/v1/items/sellable").queryParam("itemIds", itemIds).build())
                    .header(TENANT_HEADER, String.valueOf(tenantId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException | IllegalStateException | OAuth2AuthorizationException e) {
            throw new DownstreamUnavailableException("Menu is not available right now", e);
        }
    }

    // idempotent by orderId: safe to repeat after a failure
    public void writeOffStock(Long tenantId, StockConsumptionRequestDto request) {
        try {
            restClient.post()
                    .uri("/api/v1/stock-consumptions")
                    .header(TENANT_HEADER, String.valueOf(tenantId))
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            // 409 (a concurrent write-off of the same order) and 429 can pass on retry; other 4xx never will
            if (e.getStatusCode() == HttpStatus.CONFLICT || e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                throw new DownstreamUnavailableException("Stock write-off failed", e);
            }
            throw new StockWriteOffRejectedException("Stock write-off rejected: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException | IllegalStateException | OAuth2AuthorizationException e) {
            throw new DownstreamUnavailableException("Stock write-off failed", e);
        }
    }

    // empty when the venue does not exist
    public Optional<TenantDto> getTenant(Long tenantId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/v1/platform/tenants/{id}", tenantId)
                    .retrieve()
                    .body(TenantDto.class));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new DownstreamUnavailableException("Venue lookup failed", e);
        } catch (RestClientException | IllegalStateException | OAuth2AuthorizationException e) {
            throw new DownstreamUnavailableException("Venue lookup failed", e);
        }
    }
}
