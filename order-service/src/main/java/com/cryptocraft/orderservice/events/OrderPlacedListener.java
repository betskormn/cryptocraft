package com.cryptocraft.orderservice.events;

import com.cryptocraft.orderservice.kafka.OrderEventPublisher;
import com.cryptocraft.orderservice.services.StockWriteOffService;
import com.cryptocraft.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * After an order is committed: publish OrderPlaced to Kafka and write off its recipes in
 * Inventory (retried later if Inventory is down). Async, so the guest does not wait for either; the
 * order is returned as PENDING and becomes WRITTEN_OFF shortly after.
 */
@Component
@RequiredArgsConstructor
public class OrderPlacedListener {

    private final StockWriteOffService stockWriteOffService;
    private final OrderEventPublisher orderEventPublisher;

    @Value("${order.events.enabled:true}")
    private boolean eventsEnabled;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        // executor thread: the request's venue has to be set again for RLS
        TenantContext.set(event.tenantId());
        try {
            // the event first: the write-off may wait for Inventory up to its timeout
            if (eventsEnabled) {
                orderEventPublisher.publishPlaced(event.orderId());
            }
            stockWriteOffService.writeOff(event.orderId());
        } finally {
            TenantContext.clear();
        }
    }
}
