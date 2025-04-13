package org.example.order.order.domain.fulfillment.persistence;

import java.util.Deque;

public interface FulfillmentIdGenerator {

    int generateFulfillmentId();

    Deque<Integer> generateFulfillmentLineItemIds(int size);
}
