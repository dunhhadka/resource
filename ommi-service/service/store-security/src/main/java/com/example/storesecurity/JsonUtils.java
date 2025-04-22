package com.example.storesecurity;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {
    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
    }

    private JsonUtils() {
    }

    public static <T> T unmarshal(String json, Class<T> clazz) {
        return mapper.convertValue(json, clazz);
    }
}
