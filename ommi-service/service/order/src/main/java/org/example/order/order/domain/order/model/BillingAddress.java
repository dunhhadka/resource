package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;

@Getter
@Entity
@Builder
@Table(name = "billing_addresses")
@NoArgsConstructor
@AllArgsConstructor
public class BillingAddress implements OrderAddress {

    @Setter
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Order order;

    @Id
    private int id;

    @JsonUnwrapped
    @Valid
    @Embedded
    private MailingAddress address;

    public BillingAddress(
            int id,
            MailingAddress address
    ) {
        this.id = id;
        this.address = address;
    }
}
