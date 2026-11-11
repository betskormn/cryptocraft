package com.cryptocraft.pricingservice.kafka;

import com.cryptocraft.pricingservice.pricing.model.PriceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceProducer {

    private final KafkaTemplate<String, PriceEvent> kafkaTemplate;

    private static final String TOPIC = "price-updates";

    public void sendPrice(PriceEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.itemId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Kafka send failed: {}", ex.getMessage());
                    }
                });
    }
}