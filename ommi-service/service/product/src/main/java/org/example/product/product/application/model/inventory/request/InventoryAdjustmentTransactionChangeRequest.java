package org.example.product.product.application.model.inventory.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustmentTransactionChangeRequest {
    @NotNull
    private BigDecimal value;

    private BigDecimal beforeValue;

    private ValueType valueType;

    @NotNull
    private InventoryAdjustmentChangeType changeType;

    public enum ValueType {
        delta,
        fix
    }
}
