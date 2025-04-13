package org.example.product.product.application.model.inventory.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryAdjustmentTransactionRequest {
    private int locationId;

    private int inventoryItemId;

    private List<InventoryAdjustmentTransactionChangeRequest> changes;

    private InventoryAdjustmentReason reason;

    private Instant issuedAt;
}
