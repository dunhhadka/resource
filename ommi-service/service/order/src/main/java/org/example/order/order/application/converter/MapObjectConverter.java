package org.example.order.order.application.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Convert;
import lombok.SneakyThrows;
import org.example.order.order.application.utils.JsonUtils;

import java.util.Map;

@Convert
public class MapObjectConverter implements AttributeConverter<Map<String, Object>, String> {

    private final TypeReference<Map<String, Object>> MAP_OBJECT = new TypeReference<>() {
    };

    @SneakyThrows
    @Override
    public String convertToDatabaseColumn(Map<String, Object> attribute) {
        if (attribute == null) return null;
        return JsonUtils.marshal(attribute);
    }

    @SneakyThrows
    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return JsonUtils.unmarshal(dbData, MAP_OBJECT);
    }
}
