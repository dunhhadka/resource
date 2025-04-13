package org.example.order.order.application.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.order.order.application.utils.JsonUtils;

import java.util.Map;

@Slf4j
@Converter
public class StringMapConverter implements AttributeConverter<Map<String, String>, String> {

    private static final TypeReference<Map<String, String>> MAP_TYPE_REFERENCE = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null) return null;
        try {
            return JsonUtils.marshal(attribute);
        } catch (JsonProcessingException e) {
            log.error("value {} to json error", attribute);
        }
        return StringUtils.EMPTY;
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            return JsonUtils.unmarshal(dbData, MAP_TYPE_REFERENCE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
