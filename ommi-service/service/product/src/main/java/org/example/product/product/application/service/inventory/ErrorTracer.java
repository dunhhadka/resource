package org.example.product.product.application.service.inventory;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public record ErrorTracer(List<UserError> userErrors) {

    public static ErrorTracerBuilder builder() {
        return new ErrorTracerBuilder();
    }

    public static class ErrorTracerBuilder {

        private final List<UserError> userErrors;

        public ErrorTracerBuilder() {
            this.userErrors = new ArrayList<>();
        }

        public ErrorTracerBuilder addError(UserError userError) {
            this.userErrors.add(userError);
            return this;
        }

        public ErrorTracer build() {
            return new ErrorTracer(this.userErrors);
        }

        public boolean isEmpty() {
            return CollectionUtils.isEmpty(this.userErrors);
        }
    }
}
