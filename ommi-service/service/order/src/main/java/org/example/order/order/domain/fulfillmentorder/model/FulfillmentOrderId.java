package org.example.order.order.domain.fulfillmentorder.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class FulfillmentOrderId implements Serializable {
    private int storeId;
    private int id;
}
