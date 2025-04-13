package org.example.order.order.domain.fulfillment.persistence;

import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.fulfillment.model.FulfillmentId;

import java.util.List;

public interface FulfillmentRepository {
    void save(Fulfillment fulfillment);

    List<Fulfillment> getByIds(List<FulfillmentId> fulfillmentIds);
}
