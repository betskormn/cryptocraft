package com.cryptocraft.pricestreamingservice.websocket;

import com.cryptocraft.pricestreamingservice.service.PriceBroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;

import java.util.regex.Pattern;

/**
 * Serves /ws/prices/{venueSlug}: the current board of that venue first, then its live price changes.
 */
@Component
@RequiredArgsConstructor
public class PriceWebSocketHandler implements WebSocketHandler {

    // same format as tenants.slug in Inventory
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private final WebSocketSessionManager sessionManager;
    private final PriceBroadcastService broadcastService;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String path = session.getHandshakeInfo().getUri().getPath();
        String venueSlug = path.substring(path.lastIndexOf('/') + 1);
        if (!SLUG.matcher(venueSlug).matches()) {
            return session.close(CloseStatus.POLICY_VIOLATION.withReason("Unknown venue"));
        }

        Sinks.Many<String> sink = Sinks.many().unicast()
                .onBackpressureBuffer(Queues.<String>get(Queues.SMALL_BUFFER_SIZE).get());
        sessionManager.add(venueSlug, sink);
        broadcastService.snapshot(venueSlug).forEach(json -> PriceBroadcastService.emit(sink, json));

        Mono<Void> output = session.send(sink.asFlux().map(session::textMessage));
        Mono<Void> input = session.receive().then();
        // whichever ends first (client closed, or output failed) ends the connection
        return Mono.firstWithSignal(output, input)
                .doFinally(signal -> sessionManager.remove(venueSlug, sink));
    }
}
