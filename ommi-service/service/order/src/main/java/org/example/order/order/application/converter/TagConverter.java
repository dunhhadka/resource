package org.example.order.order.application.converter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TagConverter {

    public static <T> List<String> convertFromRequest(T tagInput) {
        if (tagInput == null)
            return List.of();
        if (tagInput instanceof String tagString) {
            return Arrays.stream(tagString.trim().split(","))
                    .map(String::trim)
                    .toList();
        }
        if (tagInput instanceof List<?> tagArrays) {
            return tagArrays.stream()
                    .filter(Objects::nonNull)
                    .filter(tag -> (tag instanceof String))
                    .map(String::valueOf)
                    .toList();
        }
        throw new IllegalArgumentException("invalid tags input");
    }
}
