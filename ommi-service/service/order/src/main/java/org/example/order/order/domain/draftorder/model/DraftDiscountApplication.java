package org.example.order.order.domain.draftorder.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DraftDiscountApplication {
    @Min(0)
    @NotNull
    private int index;
    @Size(max = 255)
    private String code;
    @Size(max = 255)
    private String title;
    @Size(max = 250)
    private String description;

    @NotNull
    private BigDecimal value;
    private BigDecimal maxValue;
    private BigDecimal amount;

    private ValueType valueType;

    private TargetType targetType;

    public enum TargetType {
        line_item,
        shipping_line
    }

    public enum ValueType {
        fixed_amount,
        percentage
    }
}
