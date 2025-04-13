package org.example.product.product.application.model.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.product.product.application.common.StringInList;
import org.example.product.product.domain.product.model.Variant;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class ProductVariantRequest {
    private Integer id;
    private @Size(max = 50) String barcode;
    private @Size(max = 50) String sku;
    private @DecimalMin(value = "0") @DecimalMax(value = "100000000000000") BigDecimal price;
    private @DecimalMin(value = "0") @DecimalMax(value = "100000000000000") BigDecimal compareAtPrice;
    private @Size(max = 500) String option1;
    private @Size(max = 500) String option2;
    private @Size(max = 500) String option3;
    private Boolean taxable;
    private String inventoryManagement;
    private String inventoryPolicy;
    private Integer inventoryQuantity;
    private Integer oldInventoryQuantity;
    private Integer inventoryQuantityAdjustment;
    private Boolean requireShipping;
    private Double weight;

    @Builder.Default
    private boolean sendWebhooks = true;
    private @StringInList(array = {"kg", "g"}) String weightUnit;
    private String unit;

    private Integer imagePosition;
    private Integer imageId;

    private @Size(max = 100) List<@Valid InventoryQuantityRequest> inventoryQuantities;

    private Variant.VariantType type;
}

