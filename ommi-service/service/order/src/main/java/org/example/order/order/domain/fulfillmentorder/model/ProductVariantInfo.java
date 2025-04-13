package org.example.order.order.domain.fulfillmentorder.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ProductVariantInfo {
    private Integer variantId;
    private Integer productId;
    private String variantTitle;
    private String title;
    private String sku;
    private int grams;
    private String vendor;
    private Integer inventoryItemId;
    private BigDecimal price;
    private BigDecimal discountedUnitPrice;
}
