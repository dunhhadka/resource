package org.example.order.order.application.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.order.order.domain.edit.model.OrderStagedChange;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonUtils {
    private static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return mapper;
    }

    public static <T> T unmarshal(InputStream inputStream, Class<T> clazz) throws IOException {
        return mapper.readValue(inputStream, clazz);
    }

    public static String marshal(Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

    public static <T> T unmarshal(String string, TypeReference<T> reference) throws JsonProcessingException {
        return mapper.readValue(string, reference);
    }

    public static <T> T unmarshal(String string, Class<T> clazz) throws JsonProcessingException {
        return mapper.readValue(string, clazz);
    }
}
