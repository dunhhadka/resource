package org.example.order.order.domain.fulfillmentorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "fulfillment_order_line_items")
public class FulfillmentOrderLineItem {
    @JsonIgnore
    @ManyToOne
    @Setter
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "fulfillmentOrderId", referencedColumnName = "id")
    private FulfillmentOrder fulfillmentOrder;

    @Id
    private int id;

    private int orderId;
    private int lineItemId;

    @Embedded
    private @Valid ProductVariantInfo variantInfo;

    private Integer totalQuantity;
    private Integer remainingQuantity;
    private boolean requireShipping;

    private Integer inventoryItemId;

    @CreationTimestamp
    private Instant createdOn;
    @UpdateTimestamp
    private Instant modifiedOn;

    public FulfillmentOrderLineItem(
            int id,
            int orderId,
            int lineItemId,
            ProductVariantInfo variantInfo,
            int quantity,
            Boolean requireShipping
    ) {
        this.id = id;
        this.orderId = orderId;
        this.lineItemId = lineItemId;
        this.variantInfo = variantInfo;
        this.totalQuantity = quantity;
        this.requireShipping = Optional.ofNullable(requireShipping).orElse(true);
    }

    public void fulfill() {
        this.remainingQuantity = 0;
    }

    public void fulfillAndClose(BigDecimal quantity) {
        int quantityInt = quantity.intValue();
        this.totalQuantity = this.totalQuantity - quantityInt;
        this.remainingQuantity = 0;
    }

    public void fulfill(BigDecimal quantity) {
        int quantityInt = quantity.intValue();
        this.remainingQuantity = this.remainingQuantity - quantityInt;
    }

    public void restock(int quantity) {
        this.remainingQuantity -= quantity;
        this.totalQuantity -= quantity;
    }
}
