package org.example.order.order.domain.draftorder.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class DraftOrderPricingInfo {
    /**
     * Tổng phụ đơn hàng = Tổng giá trị đơn hàng - giảm giá sản phẩm - giảm giá đơn hàng
     * = line_items_subtotal - applied_discount.amount
     */
    @NotNull
    private BigDecimal subtotalPrice;

    /**
     * Tổng số tiền của đơn đặt hàng nháp (bao gồm thuế, phí vận chuyển và giảm giá)
     * Nếu taxable = true
     * total_price = subtotal_price + total_shipping_price
     * Nếu taxable = false
     * total_price = subtotal_price + total_shipping_price + total_tax
     */
    @NotNull
    private BigDecimal totalPrice;

    /**
     * Tổng khuyến mãi bao gồm cấp đọ khuyến mãi sản phẩm và cấp độ đơn hàng
     * = Tổng LineItem.applied_discount.amount + draft_order.applied_discount.amount
     */
    private BigDecimal totalDiscounts;

    /**
     * Tổng phí vận chuyển đơn hàng
     * = shipping_line.price
     */
    private BigDecimal totalShippingPrice;

    /**
     * Tổng giá trị lineItem không bao gồm bất kỳ giảm giá nào
     * = Tổng LineItem.total_original
     */
    private BigDecimal totalLineItemPrice;

    /**
     * Tổng giá trị tất cả lineItem đã giảm gias sản phẩm nhưng chưa bao gồm khuyến mãi ở cấp độ đơn hàng
     * = Tổng LineItem.discounted_total
     */
    private BigDecimal lineItemSubtotalPrice;

    /**
     * Tổng giá trị thuế của đơn hàng
     * = Tổng tax_line.price
     */
    private BigDecimal totalTax;
}
