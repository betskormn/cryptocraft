package com.cryptocraft.orderservice.clients;

import com.cryptocraft.orderservice.clients.dto.SellableItemDto;
import com.cryptocraft.orderservice.clients.dto.StockConsumptionRequestDto;
import com.cryptocraft.orderservice.clients.dto.TenantDto;
import com.cryptocraft.orderservice.exceptions.DownstreamUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Component
public class InventoryClient {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final RestClient restClient;

    public InventoryClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
                           @Value("${order.inventory-url:http://inventory-service}") String inventoryUrl) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl(inventoryUrl).build();
    }

    // menu of the venue with stop-list state
    public List<SellableItemDto> getSellableItems(Long tenantId) {
        try {
            return restClient.get()
                    .uri("/api/v1/items/sellable")
                    .header(TENANT_HEADER, String.valueOf(tenantId))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
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
        } catch (RestClientException e) {
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
        } catch (RestClientException e) {
            throw new DownstreamUnavailableException("Venue lookup failed", e);
        }
    }
}
