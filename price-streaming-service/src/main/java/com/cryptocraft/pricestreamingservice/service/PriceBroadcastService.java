package com.cryptocraft.pricestreamingservice.service;

import com.cryptocraft.pricestreamingservice.model.PriceEvent;
import com.cryptocraft.pricestreamingservice.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static reactor.core.publisher.Mono.*;

@Service
@RequiredArgsConstructor
public class PriceBroadcastService {

    private final WebSocketSessionManager sessionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void broadcast(PriceEvent event) {
        sessionManager.getSessions().forEach(session -> {
            try {
                String json = objectMapper.writeValueAsString(event);
                session.send(just(session.textMessage(json))
                ).subscribe();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}