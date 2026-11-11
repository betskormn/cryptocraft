package com.cryptocraft.pricestreamingservice.service;

import com.cryptocraft.pricestreamingservice.model.PriceEvent;
import com.cryptocraft.pricestreamingservice.websocket.WebSocketSessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends each price event only to the screens of its own venue and remembers the latest price per
 * item, so a screen that connects gets the full board at once instead of waiting for every item to move.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceBroadcastService {

    private final WebSocketSessionManager sessionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // venue slug -> itemId -> latest event as JSON
    private final Map<String, Map<Long, String>> latestPrices = new ConcurrentHashMap<>();

    public void broadcast(PriceEvent event) {
        String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            log.error("Cannot serialize price event for item {}", event.itemId(), e);
            return;
        }
        latestPrices.computeIfAbsent(event.tenantSlug(), k -> new ConcurrentHashMap<>()).put(event.itemId(), json);
        sessionManager.getSubscribers(event.tenantSlug()).forEach(sink -> emit(sink, json));
    }

    public Collection<String> snapshot(String venueSlug) {
        Map<Long, String> prices = latestPrices.get(venueSlug);
        return prices == null ? List.of() : List.copyOf(prices.values());
    }

    /**
     * Retries only when another thread is emitting to the same sink (the snapshot on connect races with
     * Kafka). A slow screen that overflows its buffer gets an error, which closes its connection.
     */
    public static void emit(Sinks.Many<String> sink, String json) {
        sink.emitNext(json, (signalType, result) -> result == Sinks.EmitResult.FAIL_NON_SERIALIZED);
    }
}
