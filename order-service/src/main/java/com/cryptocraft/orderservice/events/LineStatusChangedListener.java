package com.cryptocraft.orderservice.events;

import com.cryptocraft.orderservice.kafka.OrderEventPublisher;
import com.cryptocraft.orderservice.kafka.OrderLineStatusChangedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes a line's new status once it is committed, for analytics (the kitchen/bar screens poll Order
 * Service themselves). The Kafka send itself is asynchronous, so the screen that made the change does not wait for it.
 */
@Component
@RequiredArgsConstructor
public class LineStatusChangedListener {

    private final OrderEventPublisher orderEventPublisher;

    @Value("${order.events.enabled:true}")
    private boolean eventsEnabled;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLineStatusChanged(OrderLineStatusChangedMessage message) {
        if (eventsEnabled) {
            orderEventPublisher.publishLineStatusChanged(message);
        }
    }
}
