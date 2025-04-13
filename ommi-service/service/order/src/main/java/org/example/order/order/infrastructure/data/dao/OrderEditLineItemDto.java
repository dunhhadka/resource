package org.example.order.order.infrastructure.data.dao;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class OrderEditLineItemDto {
    private int storeId;
    private int editingId;
    private UUID id;

    private Integer variantId;

    private Integer productId;

    private Integer locationId;

    private String sku;
    private String title;
    private String variantTitle;

    private boolean taxable;
    private boolean requireShipping;
    private boolean restockable;

    private BigDecimal editableQuantity;

    private BigDecimal originalUnitPrice;

    private BigDecimal discountedUnitPrice;

    private BigDecimal editableSubtotal;

    private boolean hasStagedDiscount;

    private Instant createdAt;
    private Instant updatedAt;

    private Integer version;
}
