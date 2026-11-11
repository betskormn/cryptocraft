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

        log.info(
                "KAFKA → WS itemId={} price={} version={}",
                event.itemId(),
                event.price(),
                event.version()
        );
        broadcaster.broadcast(event);
    }
}