package com.cryptocraft.orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    // calls to Inventory and Pricing happen while a guest waits for the order to be accepted
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    // plain builder for everything that asks for "a" RestClient.Builder, notably the Eureka client itself:
    // given the load-balanced one it would look up eureka-server in Eureka and never register
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    // load-balanced: the host in a URL is the Eureka service id (http://inventory-service/...);
    // inject with @LoadBalanced
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
