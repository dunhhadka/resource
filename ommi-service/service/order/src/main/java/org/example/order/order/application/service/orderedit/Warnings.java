package org.example.order.order.application.service.orderedit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Warnings(Map<String, List<String>> texts) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, List<String>> warnings;

        public Builder() {
            this.warnings = new LinkedHashMap<>();
        }

        public Builder add(String key, String message) {
            this.warnings.compute(key, (k, ov) -> {
                if (ov == null) return new ArrayList<>(List.of(message));
                ov.add(message);
                return ov;
            });
            return this;
        }
    }
}
