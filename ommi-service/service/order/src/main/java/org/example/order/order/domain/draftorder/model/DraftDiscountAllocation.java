package org.example.order.order.domain.draftorder.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DraftDiscountAllocation {
    private BigDecimal amount;

    private int discountApplicationIndex;
}
