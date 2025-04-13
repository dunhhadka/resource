package org.example.order.order.domain.fulfillment.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Deque;

@Repository
@RequiredArgsConstructor
public class SqlFulfillmentIdGeneratorImpl implements FulfillmentIdGenerator {

    @Override
    public int generateFulfillmentId() {
        return 0;
    }

    @Override
    public Deque<Integer> generateFulfillmentLineItemIds(int size) {
        return null;
    }
}
