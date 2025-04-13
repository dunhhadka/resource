package org.example.product.product.application.service.routing;

import java.math.BigDecimal;
import java.util.List;

public class OrderRoutingCustomItem extends RoutingItem {
    private final boolean requireShipping;

    public OrderRoutingCustomItem(int index, BigDecimal quantity, boolean requireShipping) {
        super(index, quantity);
        this.requireShipping = requireShipping;
    }

    @Override
    public boolean isRequireShipping() {
        return false;
    }

    @Override
    public List<Integer> detectLocationCanFulfill(List<LocationInfo> availableLocations) {
        return null;
    }
}
