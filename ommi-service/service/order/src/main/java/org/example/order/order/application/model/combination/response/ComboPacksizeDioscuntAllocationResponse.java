package org.example.order.order.application.model.combination.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ComboPacksizeDioscuntAllocationResponse {
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;
    private int discountApplicationId;
    private BigDecimal remainder;

    public void addAmount(BigDecimal addPrice) {
        this.amount = this.amount.add(addPrice);
    }
}
