package org.example.product.product.application.service.routing;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class InventoryLevelInfo {
    private final int id;

    private final int locationId;

    private final BigDecimal available;

    private final int inventoryItemId;
    private final int variantId;
}
