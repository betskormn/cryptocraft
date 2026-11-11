package com.cryptocraft.orderservice.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka message on topic order-events: an order was sent to the kitchen/bar. For the kitchen and bar
 * screens (MEAL / DRINK lines), demand-based pricing and analytics.
 */
public record OrderPlacedMessage(
        String type,
        UUID orderId,
        Long tenantId,
        UUID sessionId,
        String tableLabel,
        UUID participantId,
        String participantName,
        Instant placedAt,
        BigDecimal total,
        List<Line> lines
) {
    public static final String TYPE = "OrderPlaced";

    public record Line(
            Long itemId,
            String itemName,
            String itemType,
            Integer quantity,
            BigDecimal unitPrice
    ) {}
}
