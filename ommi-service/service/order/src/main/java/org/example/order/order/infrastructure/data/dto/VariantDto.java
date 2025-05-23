package org.example.order.order.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.draftorder.model.VariantType;

import java.math.BigDecimal;

@Getter
@Setter
public class VariantDto {
    private int id;
    private int productId;

    private String title;
    private String sku;
    private String unit;

    private BigDecimal price;
    private int grams;

    private String inventoryManagement;
    private String inventoryPolicy;
    private Integer inventoryItemId;

    private boolean requiresShipping;
    private boolean taxable;

    private VariantType type;
}
