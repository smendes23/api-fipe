package br.com.fipe.gateway.infrastructure.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Serializer;

public class CustomJsonSerializerAdapter<T>  implements Serializer<T> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, T data) {
        try {
            this.objectMapper.registerModule(new JavaTimeModule());
            this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Error serializing object to JSON", e);
        }
    }
}

