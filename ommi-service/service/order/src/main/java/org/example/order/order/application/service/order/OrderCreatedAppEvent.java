package org.example.order.order.application.service.order;

import lombok.Builder;
import lombok.Getter;
import org.example.order.order.application.model.fulfillmentorder.OrderRoutingResponse;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.model.order.request.OrderTransactionCreateRequest;
import org.example.order.order.application.model.order.response.OrderPaymentResult;
import org.example.order.order.domain.order.model.OrderId;

import java.util.List;

@Getter
@Builder
public class OrderCreatedAppEvent {
    private final int storeId;
    private final OrderId orderId;
    private final OrderRoutingResponse orderRoutingResponse;
    private final List<OrderCreateRequest.FulfillmentRequest> fulfillmentRequests;
    private final OrderPaymentResult paymentResult;
    private final List<OrderTransactionCreateRequest> transactions;
}
