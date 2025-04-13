package org.example.order.order.domain.edit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
@Table(name = "order_edit_tax_lines")
public class AddedTaxLine {

    @Setter
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "editingId", referencedColumnName = "id")
    private OrderEdit orderEdit;

    @Id
    private UUID id;

    @NotNull
    private String targetId;

    @NotBlank
    @Size(max = 255)
    private String title;

    private BigDecimal rate;

    @NotNull
    private BigDecimal price;

    private BigDecimal quantity;

    @NotNull
    private Instant updatedAt;

    @Version
    private Integer version;

    public AddedTaxLine(
            UUID id,
            String title,
            String targetId,
            BigDecimal quantity,
            BigDecimal rate,
            boolean taxIncluded,
            BigDecimal totalPrice,
            Currency currency
    ) {
        this.id = id;
        this.title = title;
        this.targetId = targetId;
        this.quantity = quantity;
        this.rate = rate;

        this.price = this.calculatePrice(this.rate, totalPrice, taxIncluded, currency);

        this.updatedAt = Instant.now();
    }

    private BigDecimal calculatePrice(BigDecimal rate, BigDecimal totalPrice, boolean taxIncluded, Currency currency) {
        var amount = rate.multiply(totalPrice);
        if (taxIncluded) {
            amount = amount.divide(this.rate.add(BigDecimal.ONE), currency.getDefaultFractionDigits(), RoundingMode.CEILING);
        } else {
            amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.CEILING);
        }
        return amount;
    }

    public BigDecimal updatePrice(BigDecimal lineItemSubtotal, BigDecimal quantity, boolean taxIncluded, Currency currency) {
        var oldPrice = this.price;
        this.price = this.calculatePrice(this.rate, lineItemSubtotal, taxIncluded, currency);
        this.quantity = quantity;
        this.updatedAt = Instant.now();

        return price.subtract(oldPrice);
    }
}
