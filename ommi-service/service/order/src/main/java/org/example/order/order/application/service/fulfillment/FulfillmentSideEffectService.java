package org.example.order.order.application.service.fulfillment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.example.inventory.AdjustmentRequest;
import org.example.inventory.InventoryRequest;
import org.example.inventory.InventoryTransactionLineItemRequest;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.service.fulfillmentorder.FulfillmentOrderListCreatedAddEvent;
import org.example.order.order.application.service.order.RefundCreatedAppEvent;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.fulfillment.persistence.FulfillmentRepository;
import org.example.order.order.domain.fulfillmentorder.model.AssignedLocation;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderLineItem;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.shipping.PickupAddressRequest;
import org.example.shipping.ShippingRequest;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentSideEffectService {

    private final FulfillmentRepository fulfillmentRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;
    private final OrderRepository orderRepository;

    private final FulfillService fulfillService;

    @EventListener(classes = FulfillmentOrderListCreatedAddEvent.class)
    @Transactional
    public void handleFulfillmentOrderListCreated(FulfillmentOrderListCreatedAddEvent event) {
        if (event.isWithFulfillment() && CollectionUtils.isNotEmpty(event.fulfillmentRequests())) {
            log.debug("Handle fulfillment order list created : {}", event);
            var fulfillmentOrders = fulfillmentOrderRepository.findByIds(event.fulfillmentOrderIds());
            var locationId = fulfillmentOrders.get(0).getAssignedLocationId();
            var assignedLocation = fulfillmentOrders.get(0).getAssignedLocation();
            this.fulfillService.createFromOrderRequest(
                    event.storeId(),
                    event.orderId(),
                    locationId,
                    assignedLocation,
                    event.fulfillmentOrderIds(),
                    event.fulfillmentRequests().get(0)
            );
        }
    }

    @TransactionalEventListener(classes = FulfillmentListCreatedAppEvent.class, phase = TransactionPhase.BEFORE_COMMIT)
    public void handleOrderFulfillmentListCreated(FulfillmentListCreatedAppEvent event) {
        log.debug("Handle order fulfillment list created: {}", event);

        var storeId = event.storeId();
        var orderId = event.orderId();
        var locationId = event.locationId();
        var fulfillmentIds = event.fulfillmentIds();

        var order = this.orderRepository.findById(orderId);
        var fulfillmentOrderList = this.fulfillmentOrderRepository.findByIds(event.fulfillmentOrderIds());
        var fulfillmentOrderMap = fulfillmentOrderList.stream()
                .flatMap(fulfillmentOrder -> fulfillmentOrder.getLineItems().stream())
                .collect(Collectors.groupingBy(FulfillmentOrderLineItem::getLineItemId));

        var fulfillmentList = this.fulfillmentRepository.getByIds(fulfillmentIds);

        List<AdjustmentRequest> adjustmentRequests = fulfillmentList.stream()
                .map(fulfillment -> {
                    var inventoryTransactionLineItemRequests = fulfillment.getLineItems()
                            .stream()
                            .filter(line -> {
                                var fulfillmentOrderLineItems = fulfillmentOrderMap.get(line.getLineItemId());
                                return CollectionUtils.isNotEmpty(fulfillmentOrderLineItems)
                                        && fulfillmentOrderLineItems.stream().anyMatch(ffo -> NumberUtils.isPositive(ffo.getVariantInfo().getInventoryItemId()));
                            })
                            .map(line -> {
                                var fulfillmentOrderLineItem = fulfillmentOrderMap.get(line.getLineItemId()).get(0);
                                var fulfillQuantity = BigDecimal.valueOf(fulfillmentOrderLineItem.getTotalQuantity());
                                var adjustQuantity = fulfillQuantity.negate();

                                var availableAdjustment = InventoryTransactionLineItemRequest.InventoryAdjustmentTransactionChangeRequest.builder()
                                        .value(adjustQuantity)
                                        .build();
                                var onHandAdjustment = InventoryTransactionLineItemRequest.InventoryAdjustmentTransactionChangeRequest.builder()
                                        .value(adjustQuantity)
                                        .build();
                                return InventoryTransactionLineItemRequest.builder()
                                        .changes(List.of(availableAdjustment, onHandAdjustment))
                                        .build();
                            })
                            .toList();

                    return AdjustmentRequest.builder()
                            .locationId(locationId)
                            .lineItems(inventoryTransactionLineItemRequests)
                            .build();
                })
                .toList();

        if (CollectionUtils.isEmpty(adjustmentRequests)) {
            return;
        }

        var inventoryRequest = InventoryRequest.builder()
                .adjustments(adjustmentRequests)
                .build();

    }

    @Async
    @Transactional
    @EventListener(classes = FulfillmentListCreatedAppEvent.class)
    public void handleOrderFulfillmentListCreatedAsync(FulfillmentListCreatedAppEvent event) {
        log.debug("Handle order fulfillment list created async: {}", event);
        var storeId = event.storeId();
        var locationId = event.locationId();
        var location = event.assignedLocation();
        var fulfillmentRequest = event.fulfillmentRequest();
        var fulfillmentList = fulfillmentRepository.getByIds(event.fulfillmentIds());
        var order = this.orderRepository.findById(event.orderId());
        var orderLineItemMap = order.getLineItems().stream()
                .collect(Collectors.toMap(LineItem::getId, Function.identity()));

        for (var fulfillment : fulfillmentList) {
            if (isNonShippingRequire(fulfillment.getDeliveryMethod())) {
                continue;
            }
            var lineItemRequests = fulfillment.getLineItems().stream()
                    .map(fulfillmentLineItem -> {
                        var orderLineItem = orderLineItemMap.get(fulfillmentLineItem.getLineItemId());

                        var originalTotal = orderLineItem.getPrice().multiply(BigDecimal.valueOf(orderLineItem.getQuantity()));
                        var discountedUnitPrice = orderLineItem.getDiscountedUnitPrice() != null ? orderLineItem.getDiscountedUnitPrice() : BigDecimal.ZERO;

                        var discountedTotal = discountedUnitPrice.multiply(BigDecimal.valueOf(orderLineItem.getQuantity()));

                        var variantLineItemInfo = orderLineItem.getVariantInfo();
                        return ShippingRequest.LineItemRequest.builder()
                                .productId(variantLineItemInfo.getProductId())
                                .variantId(variantLineItemInfo.getVariantId())
                                .title(variantLineItemInfo.getTitle())
                                .variantTitle(variantLineItemInfo.getVariantTitle())
                                .sku(variantLineItemInfo.getSku())
                                .price(orderLineItem.getPrice())
                                .discountedUnitPrice(discountedUnitPrice)
                                .quantity(fulfillmentLineItem.getQuantity())
                                .discountedTotal(discountedTotal)
                                .grams(variantLineItemInfo.getGrams())
                                .orderLineItemId(orderLineItem.getId())
                                .originalTotal(originalTotal)
                                .build();
                    })
                    .toList();

            var shippingRequest = ShippingRequest.builder()
                    .orderId(order.getId().getId())
                    .orderName(order.getReferenceInfo().getName())
                    .fulfillmentId(fulfillment.getId().getId())
                    .locationId(locationId)
                    .locationName(location.getName())
                    .deliveryMethod(fulfillmentRequest.getDeliveryMethod().name())
                    .deliveryStatus(fulfillmentRequest.getShippingStatus().name())
                    .sendNotification(fulfillmentRequest.isSendNotification())
                    .pickupAddress(buildPickupAddress(locationId, location, fulfillmentRequest.getPickupAddress()))
                    .shippingAddress(null)
                    .shippingInfo(null)
                    .trackingInfo(null)
                    .note(fulfillmentRequest.getNote())
                    .lineItems(lineItemRequests)
                    .build();
        }
    }

    private PickupAddressRequest buildPickupAddress(int locationId, AssignedLocation location, OrderCreateRequest.PickupAddress pickupAddress) {
        return null;
    }

    private boolean isNonShippingRequire(Fulfillment.DeliveryMethod deliveryMethod) {
        return deliveryMethod == null
                || deliveryMethod == Fulfillment.DeliveryMethod.none
                || deliveryMethod == Fulfillment.DeliveryMethod.retail
                || deliveryMethod == Fulfillment.DeliveryMethod.pick_up;
    }

    @Transactional
    @EventListener(RefundCreatedAppEvent.class)
    public void handleOrderRefundCreated(RefundCreatedAppEvent event) {
        int storeId = event.getStoreId();
        int orderId = event.getOrderId();
        var refundLineItems = event.getRestockLineItems();


    }
}
