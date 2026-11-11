package com.cryptocraft.orderservice.kafka;

import com.cryptocraft.orderservice.dao.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Best effort: a failed send is only logged. Consumers that must not miss an order (payments, analytics)
 * will need an outbox; the kitchen screens can reload open orders from this service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    static final String TOPIC = "order-events";

    private final KafkaTemplate<String, OrderPlacedMessage> kafkaTemplate;

    public void publishPlaced(Order order) {
        OrderPlacedMessage message = new OrderPlacedMessage(
                OrderPlacedMessage.TYPE,
                order.getOrderId(),
                order.getTenantId(),
                order.getSession().getSessionId(),
                order.getSession().getTable().getLabel(),
                order.getParticipant().getParticipantId(),
                order.getParticipant().getDisplayName(),
                order.getCreatedAt(),
                order.getTotal(),
                order.getLines().stream()
                        .map(line -> new OrderPlacedMessage.Line(
                                line.getItemId(),
                                line.getItemName(),
                                line.getItemType().name(),
                                line.getQuantity(),
                                line.getUnitPrice()))
                        .toList());

        // keyed by venue: one venue's orders stay in order for its screens
        kafkaTemplate.send(TOPIC, String.valueOf(order.getTenantId()), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("OrderPlaced for order:{} not sent: {}", order.getOrderId(), ex.getMessage());
                    }
                });
    }
}
