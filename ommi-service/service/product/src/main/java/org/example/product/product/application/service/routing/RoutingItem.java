package org.example.product.product.application.service.routing;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public abstract class RoutingItem {
    protected final int index;
    protected final BigDecimal quantity;

    public RoutingItem(int index, BigDecimal quantity) {
        this.index = index;
        this.quantity = quantity;
    }

    public abstract boolean isRequireShipping();

    public abstract List<Integer> detectLocationCanFulfill(List<LocationInfo> availableLocations);
}
