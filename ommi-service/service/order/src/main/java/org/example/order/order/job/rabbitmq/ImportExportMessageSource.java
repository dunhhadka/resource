package org.example.order.order.job.rabbitmq;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ImportExportMessageSource {

    private final MessageSource messageSource;

    public Map<String, Map<Object, String>> getMessageMap(Class<?> clazz) {
        ImportExportModel importExportModel = clazz.getAnnotation(ImportExportModel.class);
        String model = importExportModel.value();
        if (StringUtils.isEmpty(model)) {
            return Map.of();
        }

        Field[] fields = clazz.getDeclaredFields();
        Map<String, Map<Object, String>> result = new HashMap<>();
        for (Field field : fields) {
            Map<Object, String> resourceMapField = new LinkedHashMap<>();
            ImportExportResource importExportResource = field.getAnnotation(ImportExportResource.class);
            if (importExportResource == null) {
                continue;
            }

            String subKey = importExportResource.value();
            if (StringUtils.isBlank(subKey)) {
                continue;
            }

            Map<String, String> resouceMap;
            try {
                resouceMap = (Map<String, String>) field.get(null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            for (var entry : resouceMap.entrySet()) {
                String keyResource = String.format("%s.%s.%s", model, subKey, entry.getKey());

                var value = messageSource.getMessage(keyResource, null, entry.getValue(), LocaleContextHolder.getLocale());

                resourceMapField.put(entry.getKey(), value);
            }

            result.put(field.getName(), resourceMapField);
        }
        return result;
    }
}
