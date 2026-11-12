package com.cryptocraft.orderservice.kafka;

import com.cryptocraft.orderservice.dao.Order;
import com.cryptocraft.orderservice.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Best effort: a failed send is only logged. Consumers that must not miss an order (payments, analytics)
 * will need an outbox.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    static final String TOPIC = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public void publishPlaced(UUID orderId) {
        orderRepository.findByIdWithLines(orderId).ifPresent(this::sendPlaced);
    }

    private void sendPlaced(Order order) {
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
                                line.getLineId(),
                                line.getItemId(),
                                line.getItemName(),
                                line.getItemType().name(),
                                line.getQuantity(),
                                line.getUnitPrice()))
                        .toList());

        send(order.getTenantId(), order.getOrderId(), message);
    }

    public void publishLineStatusChanged(OrderLineStatusChangedMessage message) {
        send(message.tenantId(), message.orderId(), message);
    }

    // keyed by venue: one venue's events stay in order for its screens
    private void send(Long tenantId, UUID orderId, Object message) {
        kafkaTemplate.send(TOPIC, String.valueOf(tenantId), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("{} for order:{} not sent: {}", message.getClass().getSimpleName(), orderId, ex.getMessage());
                    }
                });
    }
}
