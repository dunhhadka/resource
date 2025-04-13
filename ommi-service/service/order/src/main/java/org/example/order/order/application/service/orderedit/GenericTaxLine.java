package org.example.order.order.application.service.orderedit;

import java.math.BigDecimal;

public interface GenericTaxLine {
    String getTitle();

    BigDecimal getRate();

    BigDecimal getPrice();

    default BigDecimal getQuantity() {
        return BigDecimal.ZERO;
    }

    default boolean isCustom() {
        return false;
    }
}
