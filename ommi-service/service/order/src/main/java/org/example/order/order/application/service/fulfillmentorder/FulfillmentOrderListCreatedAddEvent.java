package org.example.order.order.application.service.fulfillmentorder;

import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.example.order.order.domain.order.model.OrderId;

import java.util.List;

public record FulfillmentOrderListCreatedAddEvent(
        int storeId,
        OrderId orderId,
        List<FulfillmentOrderId> fulfillmentOrderIds,
        boolean isWithFulfillment,
        List<OrderCreateRequest.FulfillmentRequest> fulfillmentRequests
) {

}
