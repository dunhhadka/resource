package org.example.order.order.domain.fulfillmentorder.persistence;

import org.springframework.stereotype.Repository;

@Repository
public class SQLFulfillmentOrderIdGeneratorImpl implements FulfillmentOrderIdGenerator {
    @Override
    public int generateFulfillmentOrderId() {
        return 0;
    }

    @Override
    public int generateFulfillmentOrderLineId() {
        return 0;
    }
}
