package org.example.order.order.domain.payment.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Embeddable
public class PaymentNextAction {
    @Size(max = 255)
    private String redirectUrl;

    @Size(max = 2000)
    private String qrCode;

    @Size(max = 64)
    private String terminalId;
}
