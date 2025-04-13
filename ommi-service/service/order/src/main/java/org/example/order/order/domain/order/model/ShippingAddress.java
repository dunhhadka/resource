package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.*;

@Getter
@Entity
@Builder
@Table(name = "shipping_addresses")
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddress {
    @JsonIgnore
    @ManyToOne
    @Setter
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Order order;

    @Id
    private int id;

    @JsonUnwrapped
    @Embedded
    @Valid
    private MailingAddress address;

    public ShippingAddress(int id, MailingAddress address) {
        this.id = id;
        this.address = address;
    }
}
