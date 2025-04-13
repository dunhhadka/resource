package org.example.order.order.domain.payment.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Embeddable
public class CheckoutCompletion {
    @Size(max = 128)
    private String message;

    @Enumerated(value = EnumType.STRING)
    private Status status;

    public enum Status {
        success, failure
    }
}
