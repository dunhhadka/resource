package org.example.product.product.domain.inventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "InventoryLevels")
public class InventoryLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int storeId;
    private int inventoryItemId;
    private int productId;
    private int variantId;
    private int locationId;

    private BigDecimal onHand = BigDecimal.ZERO;
    private BigDecimal available = BigDecimal.ZERO;
    private BigDecimal committed = BigDecimal.ZERO;
    private BigDecimal incoming = BigDecimal.ZERO;

    private Instant createdAt;
    private Instant updatedAt;

    public InventoryLevel(
            int storeId,
            int productId,
            int variantId,
            int inventoryItemId,
            Integer locationId,
            BigDecimal onHand,
            BigDecimal available,
            BigDecimal committed,
            BigDecimal incoming
    ) {
        this.storeId = storeId;
        this.inventoryItemId = inventoryItemId;
        this.productId = productId;
        this.variantId = variantId;
        this.locationId = locationId;

        this.onHand = onHand;
        this.available = available;
        this.committed = committed;
        this.incoming = incoming;

        this.updatedAt = Instant.now();
        this.createdAt = Instant.now();
    }
}
