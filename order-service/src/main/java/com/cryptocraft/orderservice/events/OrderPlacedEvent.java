package com.cryptocraft.orderservice.events;

import java.util.UUID;

/**
 * Published inside the transaction that placed an order; acted upon only after it commits
 * (stock write-off, Kafka OrderPlaced), see OrderPlacedListener.
 */
public record OrderPlacedEvent(UUID orderId) {}
