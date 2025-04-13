package org.example.order.order.application.converter;

import jakarta.persistence.AttributeConverter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ListNumberAttributeConverter implements AttributeConverter<List<Integer>, String> {
    @Override
    public String convertToDatabaseColumn(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return null;
        }
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (StringUtils.isBlank(dbData) || ",".equals(dbData)) {
            return Collections.emptyList();
        }
        return Arrays.stream(dbData.split(","))
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(Integer::valueOf)
                .toList();
    }
}
