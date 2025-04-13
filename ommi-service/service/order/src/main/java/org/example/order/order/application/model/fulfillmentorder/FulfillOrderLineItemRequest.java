package org.example.order.order.application.model.fulfillmentorder;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FulfillOrderLineItemRequest {
    private int fulfillmentOrderLineItemId;
    @PositiveOrZero
    private BigDecimal quantity;
}
