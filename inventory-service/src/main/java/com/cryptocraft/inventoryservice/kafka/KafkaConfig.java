package com.cryptocraft.inventoryservice.kafka;

import com.cryptocraft.support.kafka.JacksonSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ProducerFactory<String, ItemConfigChangedEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildProducerProperties(),
                new StringSerializer(),
                new JacksonSerializer<>()
        );
    }

    @Bean
    public KafkaTemplate<String, ItemConfigChangedEvent> kafkaTemplate(
            ProducerFactory<String, ItemConfigChangedEvent> producerFactory
    ) {
        return new KafkaTemplate<>(producerFactory);
    }
}
