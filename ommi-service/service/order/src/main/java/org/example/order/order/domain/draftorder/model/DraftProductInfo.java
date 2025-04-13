package org.example.order.order.domain.draftorder.model;

import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class DraftProductInfo {
    private Integer productId;
    private Integer variantId;

    private Integer inventoryItemId;
    @Transient
    private String inventoryPolicy;
    @Transient
    private String inventoryManagement;

    private BigDecimal price;

    @NotBlank
    private String title;
    private String variantTitle;
    private String sku;

    private boolean taxable;

    private String vendor;
    private String unit;
    private String itemUnit;

    @Builder.Default
    private VariantType type = VariantType.normal;
}
