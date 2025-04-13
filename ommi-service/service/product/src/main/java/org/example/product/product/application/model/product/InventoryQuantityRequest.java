package org.example.product.product.application.model.product;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InventoryQuantityRequest {
    private long locationId;
    private BigDecimal onHand;
}
