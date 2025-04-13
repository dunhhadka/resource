package org.example.order.order.application.model.draftorder;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ShippingTax {
    private BigDecimal taxRate;
    private String taxName;
}
