package org.example.product.product.domain.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "InventoryItems")
@NoArgsConstructor
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int storeId;
    private int productId;
    private int variantId;

    @Size(max = 50)
    private String sku;
    @Size(max = 200)
    private String barcode;

    private boolean tracked;
    private boolean requireShipping;

    @NotNull
    private BigDecimal costPrice;

    private Instant createdAt;
    private Instant updatedAt;

    public InventoryItem(
            int inventoryItemId,
            int storeId,
            int productId,
            int variantId,
            String sku,
            String barcode,
            boolean tracked,
            boolean requireShipping,
            BigDecimal costPrice
    ) {
        this.id = inventoryItemId;
        this.storeId = storeId;
        this.productId = productId;
        this.variantId = variantId;
        this.sku = sku;
        this.barcode = barcode;
        this.tracked = tracked;
        this.requireShipping = requireShipping;
        this.costPrice = costPrice;
    }
}
