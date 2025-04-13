package org.example.order.order.domain.fulfillmentorder.persistence;

import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.example.order.order.domain.order.model.OrderId;

import java.util.List;

public interface FulfillmentOrderRepository {
    void save(FulfillmentOrder fulfillmentOrder);

    List<FulfillmentOrder> findByIds(List<FulfillmentOrderId> fulfillmentOrderIds);

    List<FulfillmentOrder> findByOrderId(OrderId orderId);
}
