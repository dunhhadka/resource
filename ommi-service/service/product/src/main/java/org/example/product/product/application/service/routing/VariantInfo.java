package org.example.product.product.application.service.routing;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VariantInfo {
    private final int id;
    private final String inventoryPolicy;
    private final int inventoryItemId;
}
