package org.example.product.product.application.service.routing;

import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class OrderRoutingNormalItem extends RoutingItem {
    private final int variantId;
    public OrderRoutingFetcher routingFetcher;

    public OrderRoutingNormalItem(int index, BigDecimal quantity, int variantId) {
        super(index, quantity);
        this.variantId = variantId;
    }

    public VariantInfo getVariantInfo() {
        assert routingFetcher != null;
        return routingFetcher.getVariantInfo(this);
    }

    public InventoryItemInfo getInventoryItem() {
        return this.routingFetcher.getInventoryItem(this);
    }

    @Override
    public List<Integer> detectLocationCanFulfill(List<LocationInfo> availableLocations) {
        if (this.isRequireShipping() && !this.routingFetcher.checkShippingAddressCanFulfill()) {
            throw new IllegalArgumentException();
        }

        switch (routingFetcher.behavior) {
            case bypass, decrement_ignoring_policy -> {
                return locationConnected();
            }
            case decrement_obeying_policy -> {
                return detectLocationCanFulfillWithObeyingPolicyBehavior();
            }
            default -> throw new NotImplementedException();
        }
    }

    private List<Integer> detectLocationCanFulfillWithObeyingPolicyBehavior() {
        if (isTrackingInventory()) {
            var inventoryPolicy = getInventoryPolicy();
            if (StringUtils.equals(inventoryPolicy, "continue")) {
                return locationConnected();
            } else if (StringUtils.equals(inventoryPolicy, "deny")) {
                if (checkEnoughQuantity()) {
                    return locationConnected();
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        return locationConnected();
    }

    private boolean checkEnoughQuantity() {
        var inventoryLevelStream = this.routingFetcher.getInventoryLevels(this);
        var availableQuantity = inventoryLevelStream
                .map(InventoryLevelInfo::getAvailable)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var needCheckQuantity = routingFetcher.getItems().stream()
                .filter(item -> item instanceof OrderRoutingNormalItem normalItem && normalItem.getVariantId() == this.getVariantId())
                .map(RoutingItem::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return needCheckQuantity.compareTo(availableQuantity) <= 0;
    }

    private String getInventoryPolicy() {
        return this.routingFetcher.getVariantInfo(this).getInventoryPolicy();
    }

    private boolean isTrackingInventory() {
        return this.routingFetcher.getInventoryItem(this).isTracked();
    }

    private List<Integer> locationConnected() {
        return this.routingFetcher.getInventoryLevels(this)
                .map(InventoryLevelInfo::getLocationId)
                .distinct()
                .toList();
    }

    public boolean isRequireShipping() {
        return this.routingFetcher.getInventoryItem(this).isRequireShipping();
    }
}
