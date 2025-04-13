package org.example.product.product.infrastructure.data.dto;

import lombok.Getter;
import lombok.Setter;
import org.example.product.product.domain.product.model.Variant;

import java.math.BigDecimal;

@Getter
@Setter
public class VariantDto {
    private int id;
    private long inventoryItemId;
    private String barcode;
    private String sku;
    private int storeId;
    private int productId;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String option1;
    private String option2;
    private String option3;
    private String title;
    private boolean taxable;
    private String inventoryManagement;
    private String inventoryPolicy;
    private int inventoryQuantity;
    private boolean requiresShipping;
    private int grams;
    private double weight;
    private String weightUnit;
    private String unit;
    private Integer imageId;
    private int position;
    private Variant.VariantType type;
}
