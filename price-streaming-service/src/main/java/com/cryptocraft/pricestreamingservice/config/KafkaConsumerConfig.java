package com.cryptocraft.pricestreamingservice.config;

import com.cryptocraft.pricestreamingservice.model.PriceEvent;
import com.cryptocraft.pricestreamingservice.util.JacksonDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, PriceEvent> consumerFactory() {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "price-stream-group");

        return new DefaultKafkaConsumerFactory<>(
                props,
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