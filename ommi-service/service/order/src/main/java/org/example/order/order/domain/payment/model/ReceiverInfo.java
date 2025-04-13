package org.example.order.order.domain.payment.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Embeddable
public class ReceiverInfo {
    @Size(max = 64)
    private String accountNumber;

    @Size(max = 255)
    private String description;

    @Size(max = 128)
    private String accountName;
}
