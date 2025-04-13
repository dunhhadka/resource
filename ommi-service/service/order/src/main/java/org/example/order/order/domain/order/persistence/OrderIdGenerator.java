package org.example.order.order.domain.order.persistence;

import java.util.Deque;

public interface OrderIdGenerator {

    int generateBillingAddressId();

    int generateShippingAddressId();

    Deque<Integer> generateCombinationLineIds(int size);

    Deque<Integer> generateLineItemIds(int size);

    Deque<Integer> generateTaxLineIds(int size);

    Deque<Integer> generateAttributeIds(int size);

    Deque<Integer> generateShippingLineIds(int size);

    int generateDiscountCodeId();

    Deque<Integer> generateDiscountApplicationIds(int size);

    Deque<Integer> generateDiscountAllocationIds(int size);

    int generateOrderId();

    Deque<Integer> generateReturnLineIds(int size);

    int generateAdjustmentId();

    int generateRefundId();

    Deque<Integer> generateRefundTaxLineIds(int size);
}
