package com.cryptocraft.orderservice.clients;

import com.cryptocraft.orderservice.clients.dto.CurrentPriceDto;
import com.cryptocraft.orderservice.exceptions.DownstreamUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.List;

@Component
public class PricingClient {

    private final RestClient restClient;

    public PricingClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
                         @Value("${order.pricing-url:http://pricing-service}") String pricingUrl) {
        this.restClient = loadBalancedRestClientBuilder.baseUrl(pricingUrl).build();
    }

    // price of each item at this moment; unknown items are left out
    public List<CurrentPriceDto> getCurrentPrices(Collection<Long> itemIds) {
        try {
            return restClient.get()
                    .uri(uri -> uri.path("/internal/current-prices").queryParam("itemIds", itemIds).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            throw new DownstreamUnavailableException("Prices are not available right now", e);
        }
    }
}
