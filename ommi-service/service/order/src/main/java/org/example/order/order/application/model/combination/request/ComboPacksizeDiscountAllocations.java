package org.example.order.order.application.model.combination.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
public class ComboPacksizeDiscountAllocations {
    @NotNull
    @Min(0)
    private BigDecimal amount;

    private int applicationId;

    @Builder.Default
    private BigDecimal remainder = BigDecimal.ZERO;
}
