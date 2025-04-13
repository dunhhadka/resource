package org.example.order.order.domain.draftorder.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Thành phần combo/packsize có thể có của lineItem
 */
@Builder
@Getter
public class DraftLineItemComponent {
    private int variantId;
    private int productId;
    private int grams;
    private boolean taxable;
    private boolean requireShipping;
    private Integer inventoryItemId;
    private String inventoryManagement;
    private String inventoryPolicy;
    private String title;
    private String variantTitle;
    private String sku;
    private String unit;
    private String vendor;

    private BigDecimal quantity;

    // Số lượng trong 1 combo/packsize
    private BigDecimal baseQuantity;

    private BigDecimal linePrice;
    private BigDecimal price;

    @Builder.Default
    private BigDecimal remainder = BigDecimal.ZERO;

    private boolean canBeOdd;

    @Builder.Default
    private List<DraftDiscountAllocation> discountAllocations = new ArrayList<>();

    @Builder.Default
    private List<DraftTaxLine> taxLines = new ArrayList<>();
}
