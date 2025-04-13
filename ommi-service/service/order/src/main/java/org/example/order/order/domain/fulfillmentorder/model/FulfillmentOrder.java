package org.example.order.order.domain.fulfillmentorder.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.example.order.order.infrastructure.configuration.exception.UserError;
import org.example.order.order.application.model.fulfillmentorder.FulfillOrderLineItemRequest;
import org.example.order.order.domain.fulfillmentorder.persistence.FulfillmentOrderIdGenerator;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Entity
@NoArgsConstructor
@Table(name = "fulfillment_orders")
public class FulfillmentOrder extends AggregateRoot<FulfillmentOrder> {

    @Transient
    private FulfillmentOrderIdGenerator idGenerator;

    @EmbeddedId
    private FulfillmentOrderId id;

    @Positive
    private int orderId;

    @Positive
    private int assignedLocationId;

    @Enumerated(value = EnumType.STRING)
    private ExpectedDeliveryMethod expectedDeliveryMethod;

    private boolean requireShipping;

    @Enumerated(value = EnumType.STRING)
    private InventoryBehavior inventoryBehavior;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private FulfillmentOrderStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "name", column = @Column(name = "assignedLocationName")),
            @AttributeOverride(name = "phone", column = @Column(name = "assignedLocationPhone")),
            @AttributeOverride(name = "email", column = @Column(name = "assignedLocationEmail")),
            @AttributeOverride(name = "address1", column = @Column(name = "assignedLocationAddress1")),
            @AttributeOverride(name = "address2", column = @Column(name = "assignedLocationAddress2")),
            @AttributeOverride(name = "ward", column = @Column(name = "assignedLocationWard")),
            @AttributeOverride(name = "wardCode", column = @Column(name = "assignedLocationWardCode")),
            @AttributeOverride(name = "district", column = @Column(name = "assignedLocationDistrict")),
            @AttributeOverride(name = "districtCode", column = @Column(name = "assignedLocationDistrictCode")),
            @AttributeOverride(name = "province", column = @Column(name = "assignedLocationProvince")),
            @AttributeOverride(name = "provinceCode", column = @Column(name = "assignedLocationProvinceCode")),
            @AttributeOverride(name = "city", column = @Column(name = "assignedLocationCity")),
            @AttributeOverride(name = "country", column = @Column(name = "assignedLocationCountry")),
            @AttributeOverride(name = "countryCode", column = @Column(name = "assignedLocationCountryCode")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "assignedLocationZipCode"))
    })
    private AssignedLocation assignedLocation;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "firstName", column = @Column(name = "destinationFirstName")),
            @AttributeOverride(name = "lastName", column = @Column(name = "destinationLastName")),
            @AttributeOverride(name = "email", column = @Column(name = "destinationEmail")),
            @AttributeOverride(name = "phone", column = @Column(name = "destinationPhone")),
            @AttributeOverride(name = "address1", column = @Column(name = "destinationAddress1")),
            @AttributeOverride(name = "address2", column = @Column(name = "destinationAddress2")),
            @AttributeOverride(name = "ward", column = @Column(name = "destinationWard")),
            @AttributeOverride(name = "wardCode", column = @Column(name = "destinationWardCode")),
            @AttributeOverride(name = "district", column = @Column(name = "destinationDistrict")),
            @AttributeOverride(name = "districtCode", column = @Column(name = "destinationDistrictCode")),
            @AttributeOverride(name = "province", column = @Column(name = "destinationProvince")),
            @AttributeOverride(name = "provinceCode", column = @Column(name = "destinationProvinceCode")),
            @AttributeOverride(name = "city", column = @Column(name = "destinationCity")),
            @AttributeOverride(name = "country", column = @Column(name = "destinationCountry")),
            @AttributeOverride(name = "countryCode", column = @Column(name = "destinationCountryCode")),
            @AttributeOverride(name = "latitude", column = @Column(name = "destinationLatitude")),
            @AttributeOverride(name = "longitude", column = @Column(name = "destinationLongitude")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "destinationZipCode")),
    })
    private Destination destination;

    @OneToMany(mappedBy = "fulfillmentOrder", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<FulfillmentOrderLineItem> lineItems;

    private Instant fulfillOn;

    public FulfillmentOrder(
            FulfillmentOrderId id,
            Long assignedLocationId,
            ExpectedDeliveryMethod expectedDeliveryMethod,
            boolean requireShipping,
            AssignedLocation assignedLocation,
            Destination destination,
            Instant fulfillOn,
            FulfillmentOrderIdGenerator idGenerator
    ) {
        this.id = id;
        this.assignedLocationId = Optional.ofNullable(assignedLocationId).map(Long::intValue).orElse(0);
        this.expectedDeliveryMethod = expectedDeliveryMethod;
        this.requireShipping = requireShipping;
        this.assignedLocation = assignedLocation;
        this.destination = destination;
        this.fulfillOn = fulfillOn;

        this.idGenerator = idGenerator;
    }

    public void addLineItem(FulfillmentOrderLineItem lineItem) {
        if (this.lineItems == null) this.lineItems = new ArrayList<>();
        this.lineItems.add(lineItem);
        lineItem.setFulfillmentOrder(this);
    }

    /**
     * Khi fulfill lineItem, Nếu lineItem có remainingQuantity = 0 thì remove line
     *
     * @return: <br/>
     * - pair.left: FulfillmentOrder mới có thể có
     * - pair.right: Các lineItem xử lý tương ứng với input
     */
    public Pair<FulfillmentOrder, List<FulfillmentOrderLineItem>> markAsFulfilled(List<FulfillOrderLineItemRequest> lineItemRequests) {

        validateLineItemRequests(lineItemRequests);

        var remainingLineItems = this.getRemainingLineItems();

        List<FulfillmentOrderLineItem> fulfilledLineItems = new ArrayList<>();

        if (isFulfillAllLineItems(lineItemRequests)) {
            remainingLineItems.forEach(lineItem -> {
                lineItem.fulfill();
                fulfilledLineItems.add(lineItem);
            });
            this.removeLineItemIfEmpty();
            this.status = FulfillmentOrderStatus.closed;
            return Pair.of(null, fulfilledLineItems);
        }

        if (isFulfillAndCreateNewFulfillmentOrder()) {
            return fulfillAndCreateNewFulfillmentOrder(lineItemRequests);
        }

        var remainingLineItemMap = this.getRemainingLineItemMap();
        lineItemRequests.forEach(lineItemInput -> {
            var lineItem = remainingLineItemMap.get(lineItemInput.getFulfillmentOrderLineItemId());
            if (lineItemInput.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                log.info("lineItem has quantity zero. Keeping fulfill");
                return;
            }
            lineItem.fulfill(lineItemInput.getQuantity());
            fulfilledLineItems.add(lineItem);
        });

        removeLineItemIfEmpty();

        // Auto set status
        updateStatusAfterFulfill();
        return Pair.of(null, fulfilledLineItems);
    }

    private void updateStatusAfterFulfill() {
        if (this.getRemainingLineItems().isEmpty()) {
            this.status = FulfillmentOrderStatus.closed;
        } else if (this.status == FulfillmentOrderStatus.open) {
            this.status = FulfillmentOrderStatus.in_process;
        }
    }

    /**
     * Chỉ xoá line tại fulfillmentOrder cũ, chuyển line đó quan fulfillmentOrder mới
     */
    private Pair<FulfillmentOrder, List<FulfillmentOrderLineItem>> fulfillAndCreateNewFulfillmentOrder(List<FulfillOrderLineItemRequest> lineItemRequests) {
        List<FulfillmentOrderLineItem> unFulfilledLineItems = new ArrayList<>();
        List<FulfillmentOrderLineItem> fulfilledLineItems = new ArrayList<>();

        // Xoá các line trong fulfillmentOrder cũ không có trong request
        this.getRemainingLineItems().stream()
                .filter(line -> lineItemRequests.stream().noneMatch(l -> l.getFulfillmentOrderLineItemId() == line.getId()))
                .forEach(lineItem -> {
                    unFulfilledLineItems.add(createNewFulfillmentLineItem(lineItem, lineItem.getRemainingQuantity()));
                    this.lineItems.remove(lineItem);
                });

        var remainingLineItemMap = this.getRemainingLineItemMap();
        lineItemRequests.forEach(itemInput -> {
            var lineItem = remainingLineItemMap.get(itemInput.getFulfillmentOrderLineItemId());
            var remainingQuantity = BigDecimal.valueOf(lineItem.getRemainingQuantity());
            var requestedQuantity = itemInput.getQuantity();

            if (requestedQuantity.compareTo(remainingQuantity) >= 0) {
                lineItem.fulfill();
            } else {
                lineItem.fulfillAndClose(requestedQuantity);
                var unFulfilledLineItem = createNewFulfillmentLineItem(lineItem, requestedQuantity.subtract(requestedQuantity).intValue());
                unFulfilledLineItems.add(unFulfilledLineItem);
            }
            fulfilledLineItems.add(lineItem);
        });

        if (CollectionUtils.isEmpty(unFulfilledLineItems)) {
            return Pair.of(null, fulfilledLineItems);
        }

        var newFulfillmentOrder = createNewFulfillmentOrder(this, unFulfilledLineItems);
        this.status = FulfillmentOrderStatus.closed;
        removeLineItemIfEmpty();

        return Pair.of(newFulfillmentOrder, fulfilledLineItems);
    }

    private FulfillmentOrder createNewFulfillmentOrder(FulfillmentOrder oldFulfillmentOrder, List<FulfillmentOrderLineItem> lineItems) {
        var newFulfillmentOrder = new FulfillmentOrder(
                new FulfillmentOrderId(oldFulfillmentOrder.getId().getStoreId(), this.idGenerator.generateFulfillmentOrderId()),
                (long) oldFulfillmentOrder.getAssignedLocationId(),
                oldFulfillmentOrder.getExpectedDeliveryMethod(),
                oldFulfillmentOrder.isRequireShipping(),
                oldFulfillmentOrder.getAssignedLocation(),
                oldFulfillmentOrder.getDestination(),
                Instant.now(),
                idGenerator);
        lineItems.forEach(newFulfillmentOrder::addLineItem);
        return newFulfillmentOrder;
    }

    private FulfillmentOrderLineItem createNewFulfillmentLineItem(FulfillmentOrderLineItem oldLineItem, int quantity) {
        return new FulfillmentOrderLineItem(
                this.idGenerator.generateFulfillmentOrderLineId(),
                oldLineItem.getOrderId(),
                oldLineItem.getLineItemId(),
                oldLineItem.getVariantInfo(),
                quantity,
                oldLineItem.isRequireShipping()
        );
    }

    private boolean isFulfillAndCreateNewFulfillmentOrder() {
        return this.status == FulfillmentOrderStatus.open;
    }

    private void removeLineItemIfEmpty() {
        this.lineItems.removeIf(line -> line.getRemainingQuantity() == 0 || line.getTotalQuantity() == 0);
    }

    private boolean isFulfillAllLineItems(List<FulfillOrderLineItemRequest> lineItemRequests) {
        if (CollectionUtils.isEmpty(lineItemRequests)) {
            return false;
        }

        var quantityRequestMap = getQuantityRequestMap(lineItemRequests);

        return this.getRemainingLineItems()
                .stream()
                .allMatch(lineItem -> {
                    var lineItemQuantityRequest = quantityRequestMap.get(lineItem.getId());
                    var remainingQuantity = BigDecimal.valueOf(lineItem.getRemainingQuantity());
                    return lineItemQuantityRequest != null
                            && lineItemQuantityRequest.compareTo(remainingQuantity) == 0;
                });
    }

    private Map<Integer, BigDecimal> getQuantityRequestMap(List<FulfillOrderLineItemRequest> lineItemRequests) {
        return lineItemRequests.stream()
                .collect(Collectors.groupingBy(
                        FulfillOrderLineItemRequest::getFulfillmentOrderLineItemId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                FulfillOrderLineItemRequest::getQuantity,
                                BigDecimal::add
                        )
                ));
    }

    private void validateLineItemRequests(List<FulfillOrderLineItemRequest> lineItemRequests) {
        // Nếu FulfillmentOrder đã close thì throw
        if (this.status == FulfillmentOrderStatus.closed) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("no_allowed")
                    .fields(List.of("status"))
                    .message("Cannot fulfill fulfillment_order, status is not allowed")
                    .build());
        }

        if (CollectionUtils.isEmpty(lineItemRequests)) {
            return;
        }

        var remainingLineItems = this.getRemainingLineItems();
        if (CollectionUtils.isEmpty(remainingLineItems)) {
            throw new ConstrainViolationException(UserError.builder()
                    .code("not_allowed")
                    .fields(List.of("fulfillment_order"))
                    .message("All line items have been fulfilled")
                    .build());
        }

        var remainingLineItemMap = this.getRemainingLineItemMap();

        Map<Integer, BigDecimal> requestQuantity = new HashMap<>();

        lineItemRequests.forEach(lineRequest -> {
            var lineId = lineRequest.getFulfillmentOrderLineItemId();
            var lineItem = remainingLineItemMap.get(lineId);
            if (lineItem == null) {
                throw new ConstrainViolationException(UserError.builder()
                        .code("not_exists")
                        .fields(List.of("line_item"))
                        .message("The fulfillment order line item does not exist with line_id = " + lineId)
                        .build());
            }

            var remainingQuantity = BigDecimal.valueOf(lineItem.getRemainingQuantity());
            var totalQuantityRequest = requestQuantity.getOrDefault(lineId, BigDecimal.ZERO)
                    .add(lineRequest.getQuantity());

            if (totalQuantityRequest.compareTo(remainingQuantity) > 0) {
                throw new ConstrainViolationException(UserError.builder()
                        .code("not_allowed")
                        .fields(List.of("quantity"))
                        .message("Cannot fulfill line item has quantity greater than " + remainingQuantity)
                        .build());
            }

            requestQuantity.put(lineId, totalQuantityRequest);
        });
    }


    private Map<Integer, FulfillmentOrderLineItem> getRemainingLineItemMap() {
        return this.getRemainingLineItems()
                .stream()
                .collect(Collectors.toMap(
                        FulfillmentOrderLineItem::getId,
                        Function.identity()
                ));
    }

    private List<FulfillmentOrderLineItem> getRemainingLineItems() {
        return this.lineItems.stream()
                .filter(line -> line.getRemainingQuantity() > 0)
                .toList();
    }

    public int restock(int restockQuantity, int lineItemId) {
        var fulfillmentOrderLine = this.lineItems.stream()
                .filter(ffoLine -> ffoLine.getLineItemId() == lineItemId)
                .findFirst()
                .orElse(null);
        if (fulfillmentOrderLine == null) {
            return 0;
        }

        int quantity = Math.min(restockQuantity, fulfillmentOrderLine.getRemainingQuantity());
        fulfillmentOrderLine.restock(quantity);
        return quantity;
    }

    public enum FulfillmentOrderRequestStatus {
        unsubmitted,
        submitted,
        accepted,
        rejected,
        cancellation_requested,
        cancellation_accepted,
        cancellation_rejected,
        closed
    }

    public enum FulfillmentOrderStatus {
        open,
        in_process,
        cancelled,
        scheduled,
        on_hold,
        incomplete,
        closed
    }

    public enum InventoryBehavior {
        bypass,
        decrement_ignoring_policy,
        decrement_obeying_policy,
        decrement_obeying_policy_in_specify_location
    }

    public enum ExpectedDeliveryMethod {
        none, // Không vận chuyển
        retail, // Bán tại cửa hàng bán lẻ
        pick_up, // Nhận tại cửa hàng (Mua online, nhận offline)
        external_service, // Đối tác vận chuyển tích hợp
        external_shipper // Đối tác vận chuyển ngoài
    }
}
