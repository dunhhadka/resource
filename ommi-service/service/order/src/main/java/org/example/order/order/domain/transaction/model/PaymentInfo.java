package org.example.order.order.domain.transaction.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Getter
@Embeddable
public class PaymentInfo {
    private Integer paymentId;
    private Integer paymentMethodId;
    private String paymentMethodName;
    private Integer providerId;
    private String paymentBillNumber;
    private String reference;
}
