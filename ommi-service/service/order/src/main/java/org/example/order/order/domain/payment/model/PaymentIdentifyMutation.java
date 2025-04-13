package org.example.order.order.domain.payment.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Embeddable
public class PaymentIdentifyMutation {

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private PaymentType type;

    private String checkoutToken;

    private Integer orderId;

    public enum PaymentType {
        checkout, order_payment
    }
}
