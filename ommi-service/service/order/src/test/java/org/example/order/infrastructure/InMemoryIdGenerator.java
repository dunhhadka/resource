package org.example.order.infrastructure;

import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderIdGenerator;
import org.example.order.order.domain.order.persistence.OrderIdGenerator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryIdGenerator implements FulfillmentOrderIdGenerator, OrderIdGenerator {

    private final AtomicInteger fulfillmentOrderId = new AtomicInteger(0);
    private final AtomicInteger fulfillmentOrderLineItemId = new AtomicInteger(0);
    private final AtomicInteger billingAddressId = new AtomicInteger();
    private final AtomicInteger shippingAddressId = new AtomicInteger();
    private final AtomicInteger combinationLineId = new AtomicInteger();
    private final AtomicInteger lineItemId = new AtomicInteger();
    private final AtomicInteger taxLineId = new AtomicInteger();
    private final AtomicInteger attributeId = new AtomicInteger();
    private final AtomicInteger shippingLineId = new AtomicInteger();
    private final AtomicInteger discountCodeId = new AtomicInteger();
    private final AtomicInteger discountApplicationId = new AtomicInteger();
    private final AtomicInteger discountAllocationId = new AtomicInteger();
    private final AtomicInteger orderId = new AtomicInteger();

    public Deque<Integer> getQueue(AtomicInteger idHolder, int size) {
        int maxValue = idHolder.addAndGet(size);
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = maxValue - size + 1; i <= maxValue; i++) {
            queue.add(i);
        }
        return queue;
    }


    @Override
    public int generateFulfillmentOrderId() {
        return fulfillmentOrderId.incrementAndGet();
    }

    @Override
    public int generateFulfillmentOrderLineId() {
        return fulfillmentOrderLineItemId.incrementAndGet();
    }

    @Override
    public int generateBillingAddressId() {
        return billingAddressId.incrementAndGet();
    }

    @Override
    public int generateShippingAddressId() {
        return this.shippingAddressId.incrementAndGet();
    }

    @Override
    public Deque<Integer> generateCombinationLineIds(int size) {
        return getQueue(this.combinationLineId, size);
    }

    @Override
    public Deque<Integer> generateLineItemIds(int size) {
        return getQueue(lineItemId, size);
    }

    @Override
    public Deque<Integer> generateTaxLineIds(int size) {
        return getQueue(taxLineId, size);
    }

    @Override
    public Deque<Integer> generateAttributeIds(int size) {
        return getQueue(attributeId, size);
    }

    @Override
    public Deque<Integer> generateShippingLineIds(int size) {
        return getQueue(shippingLineId, size);
    }

    @Override
    public int generateDiscountCodeId() {
        return discountCodeId.incrementAndGet();
    }

    @Override
    public Deque<Integer> generateDiscountApplicationIds(int size) {
        return getQueue(discountApplicationId, size);
    }

    @Override
    public Deque<Integer> generateDiscountAllocationIds(int size) {
        return getQueue(discountAllocationId, size);
    }

    @Override
    public int generateOrderId() {
        return orderId.incrementAndGet();
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
