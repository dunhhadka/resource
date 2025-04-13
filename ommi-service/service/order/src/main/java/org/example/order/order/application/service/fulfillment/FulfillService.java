package org.example.order.order.application.service.fulfillment;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.fulfillment.model.FulfillmentId;
import org.example.order.order.domain.fulfillment.model.FulfillmentLineItem;
import org.example.order.order.domain.fulfillment.model.OriginAddress;
import org.example.order.order.domain.fulfillment.persistence.FulfillmentIdGenerator;
import org.example.order.order.domain.fulfillment.persistence.FulfillmentRepository;
import org.example.order.order.domain.fulfillmentorder.model.AssignedLocation;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderLineItem;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FulfillService {

    private final FulfillmentIdGenerator idGenerator;
    private final FulfillmentRepository fulfillmentRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final OrderRepository orderRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    public void createFromOrderRequest(int storeId,
                                       OrderId orderId,
                                       int locationId,
                                       AssignedLocation assignedLocation,
                                       List<FulfillmentOrderId> fulfillmentOrderIds,
                                       OrderCreateRequest.FulfillmentRequest fulfillmentRequest
    ) {
        var order = this.orderRepository.findById(orderId);
        var fulfillmentOrders = fulfillmentOrderRepository.findByIds(fulfillmentOrderIds);

        var deliveryMethod = fulfillmentRequest.getDeliveryMethod();
        List<Fulfillment> fulfillmentList = new ArrayList<>();
        for (var fulfillmentOrder : fulfillmentOrders) {
            Fulfillment fulfillment = switch (deliveryMethod) {
                case external_service -> throw new ConstrainViolationException("delivery_method", "is not supported");
                case external_shipper, internal_shipper, employee, ecommerce, none, pick_up, retail, outside_shipper ->
                        buildFulfillment(storeId, order, locationId, fulfillmentOrder, fulfillmentRequest);
                default -> throw new ConstrainViolationException("delivery_method", "");
            };
            fulfillmentList.add(fulfillment);
        }

        if (CollectionUtils.isNotEmpty(fulfillmentList)) {
            fulfillmentList.forEach(this.fulfillmentRepository::save);
        }

        List<FulfillmentId> fulfillmentIds = fulfillmentList.stream()
                .map(Fulfillment::getId)
                .toList();
        var createdAppEvent = new FulfillmentListCreatedAppEvent(
                storeId,
                orderId,
                locationId,
                assignedLocation,
                deliveryMethod,
                fulfillmentOrderIds,
                fulfillmentIds,
                fulfillmentRequest
        );
        applicationEventPublisher.publishEvent(createdAppEvent);
    }

    private Fulfillment buildFulfillment(
            int storeId,
            Order order,
            int locationId,
            FulfillmentOrder fulfillmentOrder,
            OrderCreateRequest.FulfillmentRequest fulfillmentRequest
    ) {
        var deliveryMethod = fulfillmentOrder.isRequireShipping() ? fulfillmentOrder.getExpectedDeliveryMethod() : FulfillmentOrder.ExpectedDeliveryMethod.none;
        return new Fulfillment(
                new FulfillmentId(storeId, idGenerator.generateFulfillmentId()),
                order.getId().getId(),
                locationId,
                fulfillmentOrder.getId(),
                deliveryMethod,
                fulfillmentRequest.isSendNotification(),
                buildLineItem(fulfillmentOrder.getLineItems()),
                buildOriginAddress(fulfillmentOrder.getAssignedLocation(), fulfillmentRequest.getPickupAddress()),
                fulfillmentRequest.getShippingStatus()
        );
    }

    private OriginAddress buildOriginAddress(AssignedLocation assignedLocation, OrderCreateRequest.PickupAddress pickupAddress) {
        return null;
    }

    private List<FulfillmentLineItem> buildLineItem(List<FulfillmentOrderLineItem> lineItems) {
        var fulfillmentLineItemIds = this.idGenerator.generateFulfillmentLineItemIds(lineItems.size());
        return lineItems.stream()
                .map(line ->
                        new FulfillmentLineItem(
                                fulfillmentLineItemIds.removeFirst(),
                                line.getOrderId(),
                                line.getLineItemId(),
                                line.getTotalQuantity(),
                                line.getTotalQuantity()
                        ))
                .toList();
    }
}
