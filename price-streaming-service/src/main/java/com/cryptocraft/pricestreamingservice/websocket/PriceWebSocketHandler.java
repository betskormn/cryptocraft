package com.cryptocraft.pricestreamingservice.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.*;

import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class PriceWebSocketHandler implements WebSocketHandler {

    private final WebSocketSessionManager sessionManager;

    @Override
    public Mono<Void> handle(WebSocketSession session) {

        sessionManager.add(session);

        return session.receive()
                .doFinally(signal -> sessionManager.remove(session))
                .then();
    }
}