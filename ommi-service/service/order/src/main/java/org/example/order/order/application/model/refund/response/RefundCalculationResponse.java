package org.example.order.order.application.model.refund.response;

import lombok.*;
import lombok.experimental.Accessors;
import org.example.order.order.domain.refund.model.RefundLineItem;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class RefundCalculationResponse {

    private Shipping shipping;

    private List<LineItem> refundLineItems;

    private List<Transaction> transactions;

    private List<LineItem> refundableLineItems;

    @Getter
    @Setter
    public static class Transaction {
        private int orderId;
        private int parentId;
        private String gateway;
        private String kind;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal maximumRefundable = BigDecimal.ZERO;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Accessors(chain = true)
    public static class LineItem {
        private int quantity;
        private int lineItemId;
        private Integer locationId;

        private BigDecimal price = BigDecimal.ZERO;
        // = discounted_price * refund_quantity - total_cart_discount_amount
        private BigDecimal subtotal = BigDecimal.ZERO;
        // = sum(tax_line.price) / quantity * refund_quantity
        private BigDecimal totalTax = BigDecimal.ZERO;

        private BigDecimal totalCartDiscount = BigDecimal.ZERO;

        private BigDecimal originalPrice = BigDecimal.ZERO;

        private BigDecimal discountedPrice = BigDecimal.ZERO;

        private BigDecimal discountedSubtotal = BigDecimal.ZERO;

        private int maximumRefundableQuantity;

        private RefundLineItem.RestockType restockType = RefundLineItem.RestockType.no_restock;

        private boolean removal;

        private org.example.order.order.domain.order.model.LineItem orderLineItem;

        public LineItem copy() {
            return new LineItem();
        }
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    @NoArgsConstructor
    public static class Shipping {
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal tax = BigDecimal.ZERO;

        private BigDecimal maximumRefundable = BigDecimal.ZERO;
    }
}
