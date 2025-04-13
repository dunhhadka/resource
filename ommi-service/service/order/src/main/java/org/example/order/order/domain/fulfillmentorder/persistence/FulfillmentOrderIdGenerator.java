package org.example.order.order.domain.fulfillmentorder.persistence;

public interface FulfillmentOrderIdGenerator {

    int generateFulfillmentOrderId();

    int generateFulfillmentOrderLineId();
}
