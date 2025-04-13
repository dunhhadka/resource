package org.example.product.product.application.model.inventory.request;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InventoryTransactionLineItemRequest {
    @Positive
    private int inventoryItemId;

    private List<InventoryAdjustmentTransactionChangeRequest> changes;
}
