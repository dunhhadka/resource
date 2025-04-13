package org.example.order.order.application.service.orderedit;

import org.apache.commons.lang3.tuple.Pair;
import org.example.location.Location;
import org.example.order.order.domain.order.model.LineItem;
import org.example.order.order.domain.order.model.Order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OrderEditedAppEvent {

    private final int storeId;
    private final int orderId;
    private final Order order;
    private final Map<Integer, Location> availableLocations;
    private final List<LineItemEditResult> items;

    public OrderEditedAppEvent(
            Order order,
            Map<Integer, Location> availableLocations,
            List<LineItem> newLineItems,
            List<Pair<LineItem, BigDecimal>> updatedItems
    ) {
        this.storeId = order.getId().getStoreId();
        this.orderId = order.getId().getId();
        this.order = order;
        this.availableLocations = availableLocations;
        this.items = new ArrayList<>(newLineItems.size() + updatedItems.size());
        for (var newItem : newLineItems) {
            this.items.add(new LineItemEditResult(EventType.add, newItem, newItem.getEditingLocationId(), BigDecimal.valueOf(newItem.getQuantity())));
        }
        for (var changeItem : updatedItems) {
            this.items.add(new LineItemEditResult(EventType.update, changeItem.getKey(), changeItem.getKey().getEditingLocationId(), changeItem.getRight()));
        }
    }

    public record LineItemEditResult(EventType type, LineItem lineItem, int locationId, BigDecimal quantity) {
    }

    public enum EventType {
        add, update, delete
    }
}
