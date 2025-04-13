package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.AdminClient;
import org.example.location.Location;
import org.example.location.LocationFilter;
import org.example.order.order.application.model.orderedit.request.OrderEditRequest;
import org.example.order.order.application.service.order.RefundCreatedAppEvent;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.OrderEditUtils;
import org.example.order.order.domain.edit.model.OrderEdit;
import org.example.order.order.domain.edit.model.OrderStagedChange;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.persistence.OrderRepository;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderEditCommitService {

    private final OrderRepository orderRepository;

    private final AdminClient adminClient;

    private final AddService addService;

    private final QuantityService quantityService;

    private final TaxService taxService;

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void editLineItems(
            OrderId orderId,
            OrderEdit orderEdit,
            OrderEditRequest.Commit request
    ) {
        var order = checkOrderEditable(orderId);

        var changes = OrderEditUtils.groupOrderStagedChange(orderEdit.getChanges());

        var warningsBuilder = Warnings.builder();

        var locations = resolveEditingLocations(orderId.getStoreId(), changes, warningsBuilder);

        if (orderEdit.getChanges().size() >= 20) {
            warningsBuilder.add("changes",
                    """
                            Applying %s changes to Order with id %s
                            . Consider limiting number of changes handled in 1 API
                            """.formatted(orderEdit.getChanges().size(), orderId));
        }

        List<LineItem> newLineItems = this.addService.addItems(order, orderEdit, changes, locations, warningsBuilder);

        var increasedItems = this.quantityService.increaseItems(order, changes);

        this.taxService.applyTaxToEditItems(order, newLineItems, increasedItems);

        var refund = this.quantityService.decreaseItems(order, changes, warningsBuilder);

        orderRepository.save(order);

        if (!newLineItems.isEmpty() || !increasedItems.isEmpty()) {
            var orderEditedEvent = new OrderEditedAppEvent(order, locations, newLineItems, increasedItems);
            this.eventPublisher.publishEvent(orderEditedEvent);
        }

        if (refund != null) {
            var refundCreatedAppEvent = new RefundCreatedAppEvent(order, List.of(), List.of());
            this.eventPublisher.publishEvent(refundCreatedAppEvent);
        }
    }


    private Map<Integer, Location> resolveEditingLocations(int storeId, OrderEditUtils.GroupedStagedChange changes, Warnings.Builder warningsBuilder) {
        var changeSize = (int) changes.addLineItemActionsStream().count();
        if (changeSize == 0) return Map.of();

        var locationIds = changes.addLineItemActionsStream()
                .map(OrderStagedChange.AddLineItemAction::getLocationId)
                .filter(NumberUtils::isPositive)
                .distinct()
                .toList();

        var builder = LocationFilter.builder();
        boolean hasDefaultLocation = false;

        if (locationIds.size() != changeSize) {
            warningsBuilder.add("location",
                    "The number of changes is difference from the number of locations");
            hasDefaultLocation = true;
            builder.defaultLocation(true);
        }

        if (CollectionUtils.isNotEmpty(locationIds)) builder.ids(locationIds);

        var locations = this.adminClient.locationFilter(storeId, builder.build()).stream()
                .collect(Collectors.toMap(
                        location -> (int) location.getId(),
                        Function.identity(),
                        (l1, l2) -> l2,
                        HashMap::new));
        String locationNotFound = locationIds.stream()
                .filter(id -> !locations.containsKey(id))
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (!locationNotFound.isEmpty()) {
            throw new ConstrainViolationException(
                    "locations",
                    "Locations not found with " + locationNotFound
            );
        }

        if (hasDefaultLocation) {
            var defaultLocations = locations.values().stream().filter(Location::isDefaultLocation).toList();
            if (defaultLocations.isEmpty()) {
                throw new ConstrainViolationException(
                        "location",
                        "Store has no Default Location"
                );
            }

            if (defaultLocations.size() != 1) {
                warningsBuilder.add("location", "Expected exactly 1 default location");
            }

            locations.put(null, defaultLocations.get(1));
        }

        return locations;
    }

    private Order checkOrderEditable(OrderId orderId) {
        var order = this.orderRepository.findById(orderId);
        if (order == null) {
            throw new ConstrainViolationException(
                    "order",
                    "order not found"
            );
        }
        if (order.getClosedOn() != null)
            throw new ConstrainViolationException("base", "");
        if (order.getCancelledOn() != null)
            throw new ConstrainViolationException("base", "");
        return order;
    }
}
