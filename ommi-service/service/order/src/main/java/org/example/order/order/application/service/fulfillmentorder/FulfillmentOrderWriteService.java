package org.example.order.order.application.service.fulfillmentorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.model.fulfillmentorder.OrderRoutingResponse;
import org.example.order.order.application.service.order.OrderCreatedAppEvent;
import org.example.order.order.application.service.order.RefundCreatedAppEvent;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.domain.fulfillmentorder.model.*;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderIdGenerator;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderRepository;
import org.example.order.order.domain.order.model.*;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulfillmentOrderWriteService {

    private final OrderRepository orderRepository;
    private final FulfillmentOrderRepository fulfillmentOrderRepository;

    private final FulfillmentOrderIdGenerator idGenerator;

    private final ApplicationEventPublisher eventPublisher;

    @EventListener(OrderCreatedAppEvent.class)
    public void handleOrderFulfillmentAdded(OrderCreatedAppEvent event) {
        log.info("Handle order fulfillment added: {}", event);
        int storeId = event.getStoreId();
        boolean isWithFulfillment = CollectionUtils.isNotEmpty(event.getFulfillmentRequests());
        var orderId = event.getOrderId();

        var order = this.orderRepository.findById(orderId);
        var orderRoutingResponse = event.getOrderRoutingResponse();
        var shippingAddress = order.getShippingAddress();

        List<FulfillmentOrder> fulfillmentOrderList = new ArrayList<>();
        for (var routingResult : orderRoutingResponse.getResults()) {
            FulfillmentOrder requireShippingFulfillmentOrder = null;
            FulfillmentOrder nonRequireShippingFulfillmentOrder = null;

            var location = routingResult.getLocation();
            AssignedLocation assignedLocation = AssignedLocation.builder()
                    .name(location.getName())
                    .phone(location.getPhone())
                    .email(location.getEmail())
                    .address1(location.getAddress1())
                    .address2(location.getAddress2())
                    .ward(location.getWard())
                    .district(location.getDistrict())
                    .districtCode(location.getDistrictCode())
                    .province(location.getProvince())
                    .provinceCode(location.getProvinceCode())
                    .country(location.getCountry())
                    .countryCode(location.getCountryCode())
                    .zipCode(location.getZipCode())
                    .build();

            Destination destination = null;
            if (shippingAddress != null) {
                var address = shippingAddress.getAddress();
                destination = Destination.builder()
                        .firstName(address.getFirstName())
                        .lastName(address.getLastName())
                        .phone(address.getPhone())
                        .email(Optional.ofNullable(order.getCustomerInfo()).map(CustomerInfo::getEmail).orElse(null))
                        .address1(address.getAddress1())
                        .address2(address.getAddress2())
                        .ward(address.getWard())
                        .wardCode(address.getWardCode())
                        .district(address.getDistrict())
                        .districtCode(address.getDistrictCode())
                        .province(address.getProvince())
                        .provinceCode(address.getProvinceCode())
                        .country(address.getCountry())
                        .countryCode(address.getCountryCode())
                        .latitude(address.getLatitude())
                        .longitude(address.getLongitude())
                        .zipCode(address.getZip())
                        .build();
            }

            Instant fulfillOn = Instant.now();

            for (int i = 0; i < routingResult.getIndexesItems().size(); i++) {
                var item = routingResult.getIndexesItems().get(i);
                var orderLineItem = order.getLineItems().get(item.getIndex());

                var variantLineItem = orderLineItem.getVariantInfo();

                ProductVariantInfo variantInfo = ProductVariantInfo.builder()
                        .productId(variantLineItem.getProductId())
                        .variantId(variantLineItem.getVariantId())
                        .title(variantLineItem.getTitle())
                        .variantTitle(variantLineItem.getVariantTitle())
                        .sku(variantLineItem.getSku())
                        .vendor(variantLineItem.getVendor())
                        .price(orderLineItem.getPrice())
                        .discountedUnitPrice(orderLineItem.getDiscountedUnitPrice())
                        .grams(variantLineItem.getGrams())
                        .inventoryItemId(variantLineItem.getInventoryItemId())
                        .build();

                var expectedDeliveryInfo = determineExpectedDeliveryMethod(order, orderLineItem, isWithFulfillment);
                var requireShipping = expectedDeliveryInfo.getLeft();
                var expectedDeliveryMethod = expectedDeliveryInfo.getRight();

                if (requireShipping) {
                    requireShippingFulfillmentOrder = buildFulfillmentOrder(
                            storeId,
                            requireShippingFulfillmentOrder,
                            order,
                            routingResult,
                            assignedLocation,
                            destination,
                            true,
                            expectedDeliveryMethod,
                            fulfillOn
                    );
                    requireShippingFulfillmentOrder.addLineItem(buildFulfillmentOrderLineItem(orderId, orderLineItem, variantInfo, true));
                } else {
                    nonRequireShippingFulfillmentOrder = buildFulfillmentOrder(
                            storeId,
                            nonRequireShippingFulfillmentOrder,
                            order,
                            routingResult,
                            assignedLocation,
                            destination,
                            false,
                            expectedDeliveryMethod,
                            fulfillOn
                    );
                    nonRequireShippingFulfillmentOrder.addLineItem(buildFulfillmentOrderLineItem(orderId, orderLineItem, variantInfo, false));
                }
            }

            Optional.ofNullable(requireShippingFulfillmentOrder).ifPresent(fulfillmentOrderList::add);
            Optional.ofNullable(nonRequireShippingFulfillmentOrder).ifPresent(fulfillmentOrderList::add);
        }

        fulfillmentOrderList.forEach(fulfillmentOrder -> {
            if (isWithFulfillment) {
                fulfillmentOrder.markAsFulfilled(Collections.emptyList());
            }
            fulfillmentOrderRepository.save(fulfillmentOrder);
        });

        var fulfillmentOrderIds = fulfillmentOrderList.stream()
                .map(FulfillmentOrder::getId)
                .toList();
        var createdEvent = new FulfillmentOrderListCreatedAddEvent(
                storeId,
                orderId,
                fulfillmentOrderIds,
                isWithFulfillment,
                event.getFulfillmentRequests()
        );
        eventPublisher.publishEvent(createdEvent);
    }

    private FulfillmentOrderLineItem buildFulfillmentOrderLineItem(
            OrderId orderId,
            LineItem orderLineItem,
            ProductVariantInfo variantInfo,
            Boolean requireShipping
    ) {
        return new FulfillmentOrderLineItem(
                idGenerator.generateFulfillmentOrderLineId(),
                orderId.getId(),
                orderLineItem.getId(),
                variantInfo,
                orderLineItem.getQuantity(),
                requireShipping
        );
    }

    private FulfillmentOrder buildFulfillmentOrder(
            int storeId,
            FulfillmentOrder fulfillmentOrder,
            Order order,
            OrderRoutingResponse.OrderRoutingResult routingResult,
            AssignedLocation assignedLocation,
            Destination destination,
            boolean requireShipping,
            FulfillmentOrder.ExpectedDeliveryMethod expectedDeliveryMethod,
            Instant fulfillOn
    ) {
        return Optional.ofNullable(fulfillmentOrder)
                .orElse(new FulfillmentOrder(
                        new FulfillmentOrderId(storeId, idGenerator.generateFulfillmentOrderId()),
                        routingResult.getLocation().getId(),
                        expectedDeliveryMethod,
                        requireShipping,
                        assignedLocation,
                        destination,
                        fulfillOn,
                        idGenerator
                ));
    }

    private Pair<Boolean, FulfillmentOrder.ExpectedDeliveryMethod> determineExpectedDeliveryMethod(Order order, LineItem orderLineItem, boolean isWithFulfillment) {
        var channel = Optional.ofNullable(order.getTracingInfo())
                .map(TracingInfo::getSource)
                .orElse("");

        var shippingLineCodes = Optional.ofNullable(order.getShippingLines())
                .stream()
                .flatMap(Collection::stream)
                .map(ShippingLine::getCode)
                .toList();

        if (isPosOffLine(channel, isWithFulfillment)) {
            return Pair.of(false, FulfillmentOrder.ExpectedDeliveryMethod.retail);
        } else if (isFromMarketPlace(channel)) {
            var fulfillBy = shippingLineCodes.stream()
                    .filter("market_code"::equals)
                    .findFirst()
                    .orElse(null);
            if (fulfillBy != null) {
                return Pair.of(true, FulfillmentOrder.ExpectedDeliveryMethod.external_service);
            }
        }

        var requiredShipping = orderLineItem.getVariantInfo().isRequireShipping();
        var expectedDeliveryMethod = requiredShipping
                ? FulfillmentOrder.ExpectedDeliveryMethod.external_shipper
                : FulfillmentOrder.ExpectedDeliveryMethod.none;
        return Pair.of(requiredShipping, expectedDeliveryMethod);
    }

    private boolean isFromMarketPlace(String channel) {
        return List.of("shopee_alias", "tiktok_alias")
                .contains(channel);
    }

    private boolean isPosOffLine(String channel, boolean isWithFulfillment) {
        return "pos".equals(channel) && isWithFulfillment;
    }

    private static final Comparator<FulfillmentOrder> _ffoRestockComparator = Comparator
            // Sắp xếp theo status, closed thì đẩy xuống cuối
            .comparing(FulfillmentOrder::getStatus, (s1, s2) -> {
                if (s1 == s2) return 0;
                return s1 == FulfillmentOrder.FulfillmentOrderStatus.closed ? 1 : -1;
            })
            // sort fulfillmentOrder theo location_id từ bé đến lớn
            .thenComparingInt(FulfillmentOrder::getAssignedLocationId)
            // sort theo id, status != close -> id:asc, status == close -> id:desc
            .thenComparing((o1, o2) -> {
                int idCompareResult = Integer.compare(o1.getId().getId(), o2.getId().getId());
                return o1.getStatus() == FulfillmentOrder.FulfillmentOrderStatus.closed
                        ? -idCompareResult : idCompareResult;
            });

    @EventListener(RefundCreatedAppEvent.class)
    public void handleRefundRestockEvent(RefundCreatedAppEvent event) {
        log.debug("handle order restocked fulfillment order : {}", event);
        if (CollectionUtils.isEmpty(event.getRestockLineItems())) {
            return;
        }

        int storeId = event.getStoreId();
        int orderId = event.getOrderId();

        var fulfillmentOrders = this.fulfillmentOrderRepository.findByOrderId(new OrderId(storeId, orderId)).stream()
                .filter(ffo -> ffo.getLineItems().stream().anyMatch(ffoLine -> ffoLine.getRemainingQuantity() > 0))
                .sorted(_ffoRestockComparator)
                .toList();
        if (CollectionUtils.isEmpty(fulfillmentOrders)) {
            return;
        }

        List<InventoryAdjustmentItem> restockedItems = new ArrayList<>();
        List<InventoryAdjustmentItem> removedItems = new ArrayList<>();

        Map<Integer, Integer> inventoryItemMap = new HashMap<>();

        boolean hasRestocked = event.getRestockLineItems().stream()
                .anyMatch(RefundCreatedAppEvent.RestockLineItem::restocked);
        if (hasRestocked) {
            inventoryItemMap = fulfillmentOrders.stream()
                    .flatMap(ffo -> ffo.getLineItems().stream())
                    .filter(ffoLine -> NumberUtils.isPositive(ffoLine.getInventoryItemId()))
                    .collect(Collectors.toMap(
                            FulfillmentOrderLineItem::getLineItemId,
                            FulfillmentOrderLineItem::getInventoryItemId,
                            (i1, i2) -> i2
                    ));
        }

        for (var restockItem : event.getRestockLineItems()) {
            if (restockItem.fufilled() && !restockItem.restocked()) {
                continue;
            }

            if (restockItem.restocked()) {
                Integer inventoryItemId = inventoryItemMap.get(restockItem.lineItemId());
                if (inventoryItemId == null) {
                    continue;
                }
                restockedItems.add(new InventoryAdjustmentItem(
                        restockItem.lineItemId(),
                        inventoryItemId,
                        restockItem.locationId(),
                        restockItem.quantity()
                ));
            }

            if (!restockItem.fufilled()) {
                int removeQuantity = restockItem.quantity();
                for (var restockFulfillmentOrder : fulfillmentOrders) {
                    if (removeQuantity <= 0) {
                        break;
                    }

                    var restockLineItem = restockFulfillmentOrder.getLineItems().stream()
                            .filter(ffoLine -> ffoLine.getLineItemId() == restockItem.lineItemId())
                            .findFirst()
                            .orElse(null);
                    if (restockLineItem == null) {
                        continue;
                    }

                    var quantity = Math.min(removeQuantity, restockLineItem.getTotalQuantity());
                    if (quantity > 0) {
                        removeQuantity -= quantity;
                        removedItems.add(new InventoryAdjustmentItem(
                                restockItem.lineItemId(),
                                restockLineItem.getInventoryItemId(),
                                restockFulfillmentOrder.getAssignedLocationId(),
                                quantity
                        ));
                    }
                }
            }
        }

        Map<FulfillmentOrderId, FulfillmentOrder> updatableFFOs = new HashMap<>();

        // giảm total_quantity/remaining_quantity của các item chưa fulfilled
        for (var restockItem : event.getRestockLineItems()) {
            if (restockItem.fufilled()) {
                continue;
            }

            int remainingQuantity = restockItem.quantity();
            for (var restockFulfillmentOrder : fulfillmentOrders) {
                if (remainingQuantity <= 0) {
                    break;
                }
                int quantity = restockFulfillmentOrder.restock(remainingQuantity, restockItem.lineItemId());
                if (quantity > 0) {
                    remainingQuantity -= quantity;
                    updatableFFOs.put(restockFulfillmentOrder.getId(), restockFulfillmentOrder);
                }
            }
        }

        if (!updatableFFOs.isEmpty()) {
            updatableFFOs.values().forEach(this.fulfillmentOrderRepository::save);
        }


    }

    public record InventoryAdjustmentItem(
            int lineItemId,
            int inventoryItemId,
            int locationId,
            int delta
    ) {
    }
}
