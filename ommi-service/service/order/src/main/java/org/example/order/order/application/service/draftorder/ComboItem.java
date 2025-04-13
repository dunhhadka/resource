package org.example.order.order.application.service.draftorder;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class ComboItem {
    private int id;
    private int productId;
    private int variantId;
    private Integer inventoryItemId;
    private String title;
    private String status;
    private String sku;
    private String barcode;
    private String option1;
    private String option2;
    private String option3;

    private String inventoryManagement;
    private String inventoryPolicy;
    private String weightUnit;
    private String type;
    private String unit;

    private BigDecimal quantity;
    private int position;
    private int inventoryQuantity;
    private int grams;
    private double weight;
    private boolean requiresShipping;
    private boolean taxable;

    private BigDecimal price;
    private BigDecimal compareAtPrice;

    private Instant createdOn;
    private Instant modifiedOn;
}
