package com.cryptocraft.orderservice.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Kafka message on topic order-events: an order was sent to the kitchen/bar. For demand-based pricing
 * and analytics.
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

    // lineId: the screens change a line's status by it
    public record Line(
            Long lineId,
            Long itemId,
            String itemName,
            String itemType,
            Integer quantity,
            BigDecimal unitPrice
    ) {}
}
