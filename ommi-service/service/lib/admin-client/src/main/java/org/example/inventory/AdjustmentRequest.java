package org.example.inventory;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class AdjustmentRequest {
    private long locationId;
    private String reason;
    private String referenceDocumentName;
    private String referenceDocumentUrl;
    private String referenceDocumentType;
    private long referenceRootId;
    private long referenceDocumentId;
    private List<InventoryTransactionLineItemRequest> lineItems;
    private Instant issuedAt;
}
