package com.cryptocraft.pricestreamingservice.websocket;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Open WebSocket connections grouped by venue slug. Each connection is represented by the sink
 * its outgoing messages are written to.
 */
@Component
public class WebSocketSessionManager {

    private final Map<String, Set<Sinks.Many<String>>> subscribers = new ConcurrentHashMap<>();

    public void add(String venueSlug, Sinks.Many<String> sink) {
        subscribers.computeIfAbsent(venueSlug, k -> ConcurrentHashMap.newKeySet()).add(sink);
    }

    public void remove(String venueSlug, Sinks.Many<String> sink) {
        subscribers.computeIfPresent(venueSlug, (k, sinks) -> {
            sinks.remove(sink);
            return sinks.isEmpty() ? null : sinks;
        });
    }

    public Set<Sinks.Many<String>> getSubscribers(String venueSlug) {
        return subscribers.getOrDefault(venueSlug, Set.of());
    }
}
