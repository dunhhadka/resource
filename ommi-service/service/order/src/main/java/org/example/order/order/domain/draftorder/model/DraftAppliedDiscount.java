package org.example.order.order.domain.draftorder.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Builder
public class DraftAppliedDiscount {
    private String title;

    private String description;

    /**
     * Tuy theo valueType
     * - Nếu là fixed_amount => amount = value
     * - Nếu là percentage => value là %, amount là số tiền tính được
     */
    private BigDecimal value;

    private ValueType valueType;

    @Setter
    private BigDecimal amount;

    private String code;

    private boolean custom;

    public enum ValueType {
        fixed_amount, percentage
    }
}
