package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor
public class PaymentMethodInfo {
    @NotBlank
    @Size(max = 250)
    private String gateway;

    @Size(max = 250)
    private String processingMethod;

    public PaymentMethodInfo(String gateway, String processingMethod) {
        this.gateway = gateway;
        this.processingMethod = processingMethod;
    }
}
