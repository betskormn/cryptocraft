package com.cryptocraft.support.kafka;

import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.json.JsonMapper;

// JSON for Kafka message values (Jackson 3, as the services' REST layer)
public class JacksonSerializer<T> implements Serializer<T> {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }
        try {
            return jsonMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }
}
