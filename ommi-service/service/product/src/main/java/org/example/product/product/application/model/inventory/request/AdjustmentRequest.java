package org.example.product.product.application.model.inventory.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class AdjustmentRequest {
    @NotNull
    @Min(1)
    private int locationId;

    @NotNull
    private InventoryAdjustmentReason reason;

    @Size(min = 1)
    private List<InventoryTransactionLineItemRequest> lineItems;

    private String referenceDocumentName;

    private String referenceDocumentUrl;

    private Instant issuedAt;
}
