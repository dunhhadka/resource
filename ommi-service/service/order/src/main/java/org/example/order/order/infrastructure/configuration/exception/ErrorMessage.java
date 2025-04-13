package org.example.order.order.infrastructure.configuration.exception;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize(using = ErrorMessage.ErrorMessageSerialize.class)
public class ErrorMessage {
    private String message;
    private Map<String, List<String>> errors;
    private List<UserError> userErrors;

    private ErrorMessage() {
    }

    public static ErrorMessageBuilder builder() {
        return new ErrorMessageBuilder();
    }

    public static class ErrorMessageBuilder {
        private String message;
        private Map<String, List<String>> errors;
        private List<UserError> userErrors;


        public ErrorMessage build() {
            var error = new ErrorMessage();
            error.message = this.message;
            error.errors = this.errors;
            error.userErrors = this.userErrors;
            return error;
        }

        public ErrorMessageBuilder addError(String message) {
            this.message = message;
            return this;
        }

        public ErrorMessageBuilder addError(String key, String message) {
            if (errors == null)
                errors = new HashMap<>();
            errors.putIfAbsent(key, new ArrayList<>());
            errors.get(key).add(message);
            return this;
        }

        public ErrorMessageBuilder addError(UserError userError) {
            if (userErrors == null)
                userErrors = new ArrayList<>();
            userErrors.add(userError);
            return this;
        }
    }

    public static final class ErrorMessageSerialize extends JsonSerializer<ErrorMessage> {

        @Override
        public void serialize(ErrorMessage value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value.message != null) {
                gen.writeStartObject();
                gen.writeFieldName("error");
                gen.writeString(value.message);
                gen.writeEndObject();
                return;
            }

            var namingStrategy = provider.getConfig().getPropertyNamingStrategy();

            if (value.errors != null && !value.errors.isEmpty()) {
                gen.writeStartObject();
                for (var error : value.errors.entrySet()) {
                    var name = namingStrategy != null
                            ? namingStrategy.nameForField(null, null, error.getKey())
                            : error.getKey();
                    gen.writeFieldName(name);

                    gen.writeStartArray();
                    for (var message : error.getValue()) {
                        gen.writeString(message);
                    }
                    gen.writeEndArray();
                }
                gen.writeEndObject();
                return;
            }

            if (value.userErrors != null && !value.userErrors.isEmpty()) {
                gen.writeStartArray();
                for (var userError : value.userErrors) {
                    gen.writeStartObject();

                    String code = namingStrategy != null
                            ? namingStrategy.nameForField(null, null, userError.getCode())
                            : userError.getCode();
                    gen.writeStringField("code", code);
                    gen.writeStringField("message", userError.getMessage());

                    if (userError.getFields() != null && !userError.getFields().isEmpty()) {
                        gen.writeFieldName("fields");
                        gen.writeStartArray();
                        for (var field : userError.getFields()) {
                            var fieldName = namingStrategy != null
                                    ? namingStrategy.nameForField(null, null, field)
                                    : field;
                            gen.writeString(fieldName);
                        }
                        gen.writeEndArray();
                    }

                    gen.writeEndObject();
                }
                gen.writeEndArray();
                return;
            }

            gen.writeStartObject();
            gen.writeStringField("error", "no message");
            gen.writeEndObject();
        }
    }
}
