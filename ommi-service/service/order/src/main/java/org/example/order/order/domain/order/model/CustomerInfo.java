package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.order.ddd.ValueObject;

@Getter
@Setter
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInfo extends ValueObject<CustomerInfo> {
    @Min(1)
    private Integer customerId;
    @Size(max = 128)
    private String email;
    @Size(max = 21)
    private String phone;
    private boolean buyerAcceptMarketing;

    public CustomerInfo(
            String email,
            String phone,
            Integer customerId,
            boolean acceptsMarketing
    ) {
        this.customerId = customerId;
        this.email = email;
        this.phone = phone;
        this.buyerAcceptMarketing = acceptsMarketing;
    }
}
