package com.briefbytes.news.orchestrator.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

public class JsonDeserializer<T> implements Deserializer<T> {

    private ObjectMapper objectMapper = new ObjectMapper();
    private Class<T> toClass;

    public JsonDeserializer(Class<T> toClass) {
        this.toClass = toClass;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(data, toClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
