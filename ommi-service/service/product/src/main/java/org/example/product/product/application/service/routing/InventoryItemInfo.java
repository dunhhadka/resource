package org.example.product.product.application.service.routing;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InventoryItemInfo {
    private final int id;
    private final int variantId;
    private final boolean tracked;
    private final boolean requireShipping;
}
