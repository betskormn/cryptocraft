package com.cryptocraft.pricestreamingservice.config;

import com.cryptocraft.pricestreamingservice.model.PriceEvent;
import com.cryptocraft.pricestreamingservice.util.JacksonDeserializer;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    // bootstrap servers, group id and offset reset come from spring.kafka.* (per profile)
    private final KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, PriceEvent> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(),
                new StringDeserializer(),
                new JacksonDeserializer<>(PriceEvent.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PriceEvent>
    kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PriceEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
