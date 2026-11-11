package com.cryptocraft.orderservice.events;

import com.cryptocraft.orderservice.kafka.OrderEventPublisher;
import com.cryptocraft.orderservice.repositories.OrderRepository;
import com.cryptocraft.orderservice.services.StockWriteOffService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * After an order is committed: write off its recipes in Inventory (retried later if Inventory is down)
 * and tell the kitchen/bar screens through Kafka. Runs in the request thread, so the venue is still set.
 */
@Component
@RequiredArgsConstructor
public class OrderPlacedListener {

    private final StockWriteOffService stockWriteOffService;
    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    @Value("${order.events.enabled:true}")
    private boolean eventsEnabled;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void onOrderPlaced(OrderPlacedEvent event) {
        stockWriteOffService.writeOff(event.orderId());
        if (eventsEnabled) {
            orderRepository.findByIdWithLines(event.orderId()).ifPresent(orderEventPublisher::publishPlaced);
        }
    }
}
