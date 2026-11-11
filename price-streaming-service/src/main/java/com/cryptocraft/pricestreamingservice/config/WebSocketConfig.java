package com.cryptocraft.pricestreamingservice.config;

import com.cryptocraft.pricestreamingservice.websocket.PriceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

import org.springframework.context.annotation.Bean;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig implements WebFluxConfigurer {

    private final PriceWebSocketHandler handler;

    @Bean
    public SimpleUrlHandlerMapping handlerMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of(
                "/ws/prices/*", handler
        ));
        mapping.setOrder(1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}