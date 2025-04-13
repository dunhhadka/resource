package org.example.order.order.application.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.refund.model.OrderAdjustment;
import org.example.order.order.domain.refund.model.Refund;
import org.example.order.order.domain.transaction.model.OrderTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class OrderHelper {

    public static MoneyInfo recalculateMoneyInfo(Order order, List<OrderTransaction> transactions) {
        var currentCartLevelDiscount = getCurrentCartLevelDiscount(order);
        var currentDiscountedTotal = getCurrentDiscountedTotal(order);
        var currentSubtotalPrice = currentDiscountedTotal.subtract(currentCartLevelDiscount);

        var shippingDiscount = getShippingDiscount(order);

        var currentCartDiscount = currentCartLevelDiscount.add(shippingDiscount);

        var totalRefundedShipping = getTotalRefundedShippintg(order);

        var currentShippingDiscountedPrice = getTotalShippingPrice(order)
                .subtract(totalRefundedShipping)
                .subtract(shippingDiscount);
        var currentTotalPrice = currentSubtotalPrice.add(currentShippingDiscountedPrice);

        var totalReceived = BigDecimal.ZERO;
        var totalRefunded = BigDecimal.ZERO;
        var nonCaptureAuthorizationAmount = BigDecimal.ZERO;
        for (var transaction : transactions) {
            if (!OrderTransaction.Status.success.equals(transaction.getStatus())) {
                continue;
            }
            switch (transaction.getKind()) {
                case sale -> totalReceived = totalReceived.add(transaction.getAmount());
                case capture -> {
                    totalReceived = totalReceived.add(transaction.getAmount());
                    if (transaction.getParentId() != null) {
                        nonCaptureAuthorizationAmount = nonCaptureAuthorizationAmount.subtract(transaction.getAmount());
                    }
                }
                case refund -> totalRefunded = totalRefunded.add(transaction.getAmount());
                case authorization ->
                        nonCaptureAuthorizationAmount = nonCaptureAuthorizationAmount.add(transaction.getAmount());
            }
        }
        var totalRefundedDiscrepancy = BigDecimal.ZERO;
        if (CollectionUtils.isNotEmpty(order.getRefunds())) {
            totalRefundedDiscrepancy = totalRefundedDiscrepancy.add(
                    order.getRefunds().stream()
                            .filter(refund -> CollectionUtils.isNotEmpty(refund.getOrderAdjustments()))
                            .map(Refund::getTotalDiscrepancy)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            );
        }

        var netPay = totalReceived.subtract(totalRefunded);

        var unpaidAmount = currentTotalPrice.subtract(netPay).add(totalRefundedDiscrepancy);


        var totalOutstanding = unpaidAmount.subtract(nonCaptureAuthorizationAmount.abs());

        return order.getMoneyInfo().toBuilder()
                .currentTotalPrice(currentTotalPrice)
                .currentSubtotalPrice(currentSubtotalPrice)
                .currentCartDiscountAmount(currentCartDiscount)
                .currentTotalDiscounts(currentDiscountedTotal)
                .totalReceived(totalReceived)
                .totalRefunded(totalRefunded)
                .netPayment(netPay)
                .unpaidAmount(unpaidAmount)
                .totalOutstanding(totalOutstanding)
                .build();
    }

    private static BigDecimal getTotalShippingPrice(Order order) {
        if (CollectionUtils.isEmpty(order.getShippingLines())) {
            return BigDecimal.ZERO;
        }
        return order.getShippingLines().stream()
                .map(ShippingLine::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal getTotalRefundedShippintg(Order order) {
        if (CollectionUtils.isEmpty(order.getRefunds())) {
            return BigDecimal.ZERO;
        }

        return order.getRefunds().stream()
                .filter(refund -> CollectionUtils.isNotEmpty(refund.getOrderAdjustments()))
                .map(Refund::getTotalShippingRefund)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal getShippingDiscount(Order order) {
        if (CollectionUtils.isEmpty(order.getShippingLines())) {
            return BigDecimal.ZERO;
        }

        return order.getShippingLines().stream()
                .filter(shipping -> !CollectionUtils.isEmpty(shipping.getDiscountAllocations()))
                .flatMap(shipping -> shipping.getDiscountAllocations().stream())
                .map(DiscountAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal getCurrentDiscountedTotal(Order order) {
        var discountedTotal = order.getLineItems().stream()
                .map(LineItem::getDiscountedTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (CollectionUtils.isEmpty(order.getRefunds())) {
            return discountedTotal;
        }

        var refundedLineItemSubtotal = order.getRefunds().stream()
                .map(Refund::getLineItemSubtotalRefunded)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var refundedCartLevelDiscount = order.getRefunds().stream()
                .map(Refund::getTotalCartDiscountRefund)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return discountedTotal
                .subtract(refundedLineItemSubtotal)
                .subtract(refundedCartLevelDiscount);
    }

    private static BigDecimal getCurrentCartLevelDiscount(Order order) {
        var cartLevelDiscount = order.getLineItems().stream()
                .map(LineItem::getOrderDiscount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (CollectionUtils.isEmpty(order.getRefunds())) {
            return cartLevelDiscount;
        }

        var refundCartLevelDiscount = order.getRefunds().stream()
                .map(Refund::getTotalCartDiscountRefund)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return cartLevelDiscount.subtract(refundCartLevelDiscount);
    }
}
