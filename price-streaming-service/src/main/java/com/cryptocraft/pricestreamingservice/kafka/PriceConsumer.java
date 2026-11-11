package com.cryptocraft.pricestreamingservice.kafka;

import com.cryptocraft.pricestreamingservice.model.PriceEvent;
import com.cryptocraft.pricestreamingservice.service.PriceBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceConsumer {

    private final PriceBroadcastService broadcaster;

    @KafkaListener(
            topics = "price-updates",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPrice(PriceEvent event) {
        // events published before venues existed carry no slug and cannot be routed to a screen
        if (event.tenantSlug() == null) {
            log.warn("Skipping price event without venue: itemId={}", event.itemId());
            return;
        }

        log.debug(
                "KAFKA → WS venue={} itemId={} price={} version={}",
                event.tenantSlug(),
                event.itemId(),
                event.price(),
                event.version()
        );
        broadcaster.broadcast(event);
    }
}