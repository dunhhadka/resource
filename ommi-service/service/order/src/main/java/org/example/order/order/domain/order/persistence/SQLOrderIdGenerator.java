package org.example.order.order.domain.order.persistence;

import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.Deque;

@Repository
public class SQLOrderIdGenerator implements OrderIdGenerator {

    @Override
    public int generateBillingAddressId() {
        return 0;
    }

    @Override
    public int generateShippingAddressId() {
        return 0;
    }

    @Override
    public Deque<Integer> generateCombinationLineIds(int size) {
        return new ArrayDeque<>();
    }

    @Override
    public Deque<Integer> generateLineItemIds(int size) {
        return new ArrayDeque<>();
    }

    @Override
    public Deque<Integer> generateTaxLineIds(int size) {
        return new ArrayDeque<>();
    }

    @Override
    public Deque<Integer> generateAttributeIds(int size) {
        return new ArrayDeque<>();
    }

    @Override
    public Deque<Integer> generateShippingLineIds(int size) {
        return new ArrayDeque<>();
    }

    @Override
    public int generateDiscountCodeId() {
        return 0;
    }

    @Override
    public Deque<Integer> generateDiscountApplicationIds(int size) {
        return new ArrayDeque<>();
    }

    @Override
    public Deque<Integer> generateDiscountAllocationIds(int size) {
        return new ArrayDeque<>();
    }

    @Override
    public int generateOrderId() {
        return 0;
    }

    @Override
    public Deque<Integer> generateReturnLineIds(int size) {
        return null;
    }

    @Override
    public int generateAdjustmentId() {
        return 0;
    }

    @Override
    public int generateRefundId() {
        return 0;
    }

    @Override
    public Deque<Integer> generateRefundTaxLineIds(int size) {
        return null;
    }
}
