package com.cryptocraft.orderservice.kafka;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka message on topic order-events: the kitchen/bar moved a line (PREPARING, READY, SERVED) or
 * cancelled it. For analytics (preparation times, cancellations).
 */
public record OrderLineStatusChangedMessage(
        String type,
        Long tenantId,
        UUID orderId,
        Long lineId,
        String itemType,
        String status,
        String cancelReason,
        Instant changedAt
) {
    public static final String TYPE = "OrderLineStatusChanged";
}
