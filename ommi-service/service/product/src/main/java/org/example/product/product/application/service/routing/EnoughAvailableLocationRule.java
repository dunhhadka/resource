package org.example.product.product.application.service.routing;

import org.apache.commons.lang3.tuple.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnoughAvailableLocationRule extends RankingLocationRule {

    @Override
    public List<RankingLocationResult> process(List<FulfillmentGroup> fulfillmentGroups) {
        var results = new ArrayList<RankingLocationResult>();
        for (var fulfillmentGroup : fulfillmentGroups) {
            var locationRanks = new ArrayList<Pair<Integer, Integer>>();

            var normalItems = fulfillmentGroup.getItems().stream()
                    .filter(OrderRoutingNormalItem.class::isInstance)
                    .map(OrderRoutingNormalItem.class::cast)
                    .toList();
            for (var locationId : fulfillmentGroup.getLocationIds()) {
                if (checkLocationAvailableCanFulfillAllItems(locationId, normalItems)) {
                    locationRanks.add(Pair.of(locationId, 0));
                } else {
                    locationRanks.add(Pair.of(locationId, 1));
                }
            }

            results.add(new RankingLocationResult(fulfillmentGroup.getId().toString(), locationRanks));
        }
        return results;
    }

    private boolean checkLocationAvailableCanFulfillAllItems(Integer locationId, List<OrderRoutingNormalItem> normalItems) {
        var normalItemGroups = normalItems.stream()
                .collect(Collectors.groupingBy(OrderRoutingNormalItem::getVariantId));
        for (var entry : normalItemGroups.entrySet()) {
            var lineItemQuantity = entry.getValue().stream()
                    .map(RoutingItem::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            var normalLine = entry.getValue().get(0);

            var inventoryLevels = normalLine.routingFetcher.getInventoryLevels(normalLine);
            var availableQuantity = inventoryLevels.map(InventoryLevelInfo::getAvailable).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (lineItemQuantity.compareTo(availableQuantity) > 0) {
                return false;
            }
        }

        return true;
    }
}
