package org.example.order.order.infrastructure.configuration.exception;

import lombok.Getter;

/**
 * Các option để throw
 * - key, message
 * - userError: code, message, fields
 */
@Getter
public class ConstrainViolationException extends RuntimeException {
    private final ErrorMessage errorMessage;

    public ConstrainViolationException(String key, String message) {
        this.errorMessage = ErrorMessage.builder().addError(key, message).build();
    }

    public ConstrainViolationException(UserError userError) {
        this.errorMessage = ErrorMessage.builder().addError(userError).build();
    }

    public ConstrainViolationException(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }
}
