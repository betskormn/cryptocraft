package com.cryptocraft.pricingservice.kafka;

import com.cryptocraft.pricingservice.dto.ItemConfigChangedEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    // bootstrap servers, group id and offset reset come from spring.kafka.* (per profile)
    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, ItemConfigChangedEvent> itemConfigConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(),
                new StringDeserializer(),
                new JacksonDeserializer<>(ItemConfigChangedEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ItemConfigChangedEvent>
    itemConfigListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ItemConfigChangedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(itemConfigConsumerFactory());
        return factory;
    }
}
