package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.ddd.ValueObject;

import java.math.BigDecimal;
import java.util.Currency;

@Getter
@Embeddable
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MoneyInfo extends ValueObject<MoneyInfo> {
    @NotNull
    private BigDecimal totalPrice;

    @NotNull
    private BigDecimal subtotalPrice;

    @NotNull
    private BigDecimal totalLineItemPrice;

    @Builder.Default
    private BigDecimal originalTotalPrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal cartDiscountAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalDiscounts = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalShippingPrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentTotalPrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentSubtotalPrice = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentTotalDiscounts = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentCartDiscountAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal currentTotalTax = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalOutstanding = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal unpaidAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalRefunded = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal totalReceived = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal netPayment = BigDecimal.ZERO;

    private @NotNull Currency currency;
}
