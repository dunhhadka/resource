package org.example.inventory;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class InventoryTransactionLineItemRequest {
    private int inventoryItemId;
    private List<InventoryAdjustmentTransactionChangeRequest> changes;

    @Getter
    @Builder
    public static class InventoryAdjustmentTransactionChangeRequest {
        private BigDecimal value;
        private String valueType;
        private String changeType;
    }
}
