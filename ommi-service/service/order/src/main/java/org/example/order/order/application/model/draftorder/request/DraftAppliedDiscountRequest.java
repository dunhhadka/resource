package org.example.order.order.application.model.draftorder.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.draftorder.model.DraftAppliedDiscount;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class DraftAppliedDiscountRequest {
    @Size(max = 255)
    private String title;

    @Size(max = 255)
    private String description;

    @NotNull
    private BigDecimal value;

    private DraftAppliedDiscount.ValueType valueType;

    @ReadOnlyProperty
    private BigDecimal amount;

    private boolean custom;
}
