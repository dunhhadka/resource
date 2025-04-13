package org.example.order.order.application.service.fulfillment;

import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.fulfillment.model.FulfillmentId;
import org.example.order.order.domain.fulfillmentorder.model.AssignedLocation;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.example.order.order.domain.order.model.OrderId;

import java.util.List;

public record FulfillmentListCreatedAppEvent(
        int storeId,
        OrderId orderId,
        int locationId,
        AssignedLocation assignedLocation,
        Fulfillment.DeliveryMethod deliveryMethod,
        List<FulfillmentOrderId> fulfillmentOrderIds,
        List<FulfillmentId> fulfillmentIds,
        OrderCreateRequest.FulfillmentRequest fulfillmentRequest
) {

}
