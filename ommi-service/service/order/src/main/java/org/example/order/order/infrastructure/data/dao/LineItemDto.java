package org.example.order.order.infrastructure.data.dao;

import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.LineItem;

import java.math.BigDecimal;

@Getter
@Setter
public class LineItemDto {
    private int storeId;
    private int orderId;
    private int id;

    private int quantity;

    private BigDecimal price;

    private int fulfillableQuantity;

    private LineItem.FulfillmentStatus fulfillmentStatus;

    private Integer variantId;
    private Integer productId;
    private boolean productExists;

    private String name;
    private String title;
    private String variantTitle;
    private String vendor;
    private String sku;

    private int grams;
    private boolean requireShipping;

    private String inventoryManagement;
    private boolean restockable;

    private Integer inventoryItemId;
    private String unit;

    private boolean taxable;

    private BigDecimal discountedUnitPrice;

    private BigDecimal discountedTotal;

    private BigDecimal originalTotal;

    private int currentQuantity;

    private int nonFulfillableQuantity;

    private int refundableQuantity;

    private String combinationLineKey;

    private Integer version;
}
