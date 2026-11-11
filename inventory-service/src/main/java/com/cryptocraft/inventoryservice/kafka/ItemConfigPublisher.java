package com.cryptocraft.inventoryservice.kafka;

import com.cryptocraft.inventoryservice.events.ItemChangedEvent;
import com.cryptocraft.inventoryservice.repositories.ItemPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Tells Pricing (and through it the TV screens) about an item change right away instead of waiting for
 * its periodic refresh. Runs after commit, so the message never describes a rolled-back change; a failed
 * send is only logged, Pricing's periodic refresh is the fallback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inventory.events.enabled", havingValue = "true", matchIfMissing = true)
public class ItemConfigPublisher {

    static final String TOPIC = "item-config-updates";

    private final KafkaTemplate<String, ItemConfigChangedEvent> kafkaTemplate;
    private final ItemPriceRepository itemPriceRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onItemChanged(ItemChangedEvent event) {
        ItemConfigChangedEvent message = itemPriceRepository.findByItemId(event.itemId())
                .map(config -> new ItemConfigChangedEvent(event.itemId(), false, config))
                .orElseGet(() -> new ItemConfigChangedEvent(event.itemId(), true, null));

        kafkaTemplate.send(TOPIC, String.valueOf(event.itemId()), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Item config event for item:{} not sent: {}", event.itemId(), ex.getMessage());
                    }
                });
    }
}
