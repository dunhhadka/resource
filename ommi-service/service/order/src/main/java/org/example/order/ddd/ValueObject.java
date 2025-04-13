package org.example.order.ddd;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
public abstract class ValueObject<T> {

    public boolean sameAs(T other) {
        return Objects.equals(this, other);
    }

    @SneakyThrows
    public List<Triple<String, Object, Object>> getDiffs(T other) {
        if (log.isDebugEnabled()) {
            log.debug("Getting Differences");
        }

        if (this.sameAs(other)) {
            if (log.isDebugEnabled()) {
                log.debug("No Differences for compare");
            }
            return List.of();
        }

        var result = new ArrayList<Triple<String, Object, Object>>();
        for (var field : this.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value1 = field.get(this);
            Object value2 = field.get(other);
            if (!Objects.equals(value1, value2)) {
                result.add(Triple.of(field.getName(), value1, value2));
            }
        }
        return result;
    }
}
