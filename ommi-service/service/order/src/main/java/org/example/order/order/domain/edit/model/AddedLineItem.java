package org.example.order.order.domain.edit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "order_edit_line_items")
public class AddedLineItem {

    @Setter
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "editingId", referencedColumnName = "id")
    private OrderEdit orderEdit;

    @Id
    private UUID id;

    @Min(0)
    private Integer variantId;

    @Min(0)
    private Integer productId;

    private Integer locationId;

    @Size(max = 500)
    private String sku;
    @NotBlank
    @Size(max = 500)
    private String title;
    @Size(max = 500)
    private String variantTitle;

    private boolean taxable;
    private boolean requireShipping;
    private boolean restockable;

    @Min(0)
    private BigDecimal editableQuantity;

    @NotNull
    private BigDecimal originalUnitPrice;

    @NotNull
    private BigDecimal discountedUnitPrice;

    @NotNull
    private BigDecimal editableSubtotal;

    private boolean hasStagedDiscount;

    @NotNull
    private Instant createdAt;
    @NotNull
    private Instant updatedAt;

    @Version
    private Integer version;

    public AddedLineItem(
            UUID id,
            Integer variantId,
            Integer productId,
            Integer locationId,
            String title,
            String variantTitle,
            boolean taxable,
            boolean requiresShipping,
            boolean restockable,
            BigDecimal quantity,
            BigDecimal price
    ) {
        this.id = id;

        this.variantId = variantId;
        this.productId = productId;
        this.locationId = locationId;

        this.title = title;
        this.variantTitle = variantTitle;

        this.taxable = taxable;
        this.requireShipping = requiresShipping;
        this.restockable = restockable;

        this.editableQuantity = quantity;
        this.originalUnitPrice = price;
        this.discountedUnitPrice = price;

        this.calculatePrice();

        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    private void calculatePrice() {
        this.editableSubtotal = this.discountedUnitPrice.multiply(this.editableQuantity);
    }

    public void adjustQuantity(BigDecimal quantity) {
        this.editableQuantity = quantity;
        this.calculatePrice();
        this.updatedAt = Instant.now();
    }

    public BigDecimal getTotalDiscount() {
        if (this.discountedUnitPrice.compareTo(this.originalUnitPrice) == 0) {
            return BigDecimal.ZERO;
        }
        var discountAmount = this.originalUnitPrice.subtract(this.discountedUnitPrice);
        return discountAmount.multiply(this.editableQuantity);
    }

    public BigDecimal removeDiscount() {
        BigDecimal prevTotalPrice = this.editableSubtotal;
        this.discountedUnitPrice = this.originalUnitPrice;
        this.calculatePrice();
        this.updatedAt = Instant.now();
        return this.editableSubtotal.subtract(prevTotalPrice);
    }

    public void applyDiscount(BigDecimal amount, Currency currency) {
        this.hasStagedDiscount = true;
        BigDecimal totalDiscountedPrice = this.originalUnitPrice.multiply(editableQuantity)
                .subtract(amount);
        this.discountedUnitPrice = totalDiscountedPrice.divide(editableQuantity, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
        this.calculatePrice();
        this.updatedAt = Instant.now();
    }
}
