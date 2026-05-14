package com.cryptocraft.pricingservice.kafka;

import com.cryptocraft.pricingservice.pricing.model.PriceEvent;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;

import java.util.Map;

@Configuration
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ProducerFactory<String, PriceEvent> producerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();

        return new DefaultKafkaProducerFactory<>(
                props,
                new StringSerializer(),
                new JacksonSerializer<>()
        );
    }

    @Bean
    public KafkaTemplate<String, PriceEvent> kafkaTemplate(
            ProducerFactory<String, PriceEvent> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}