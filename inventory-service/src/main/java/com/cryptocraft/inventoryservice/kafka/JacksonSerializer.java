package com.cryptocraft.inventoryservice.kafka;

import org.apache.kafka.common.serialization.Serializer;
import tools.jackson.databind.json.JsonMapper;

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
