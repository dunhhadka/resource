package org.example.product.product.application.service.routing;

import java.util.ArrayList;
import java.util.List;

public class OrderRoutingMatrix {

    private final boolean[][] matrix;
    private final int locationSize;
    private final int itemSize;
    private final List<LocationInfo> locations;
    private final List<RoutingItem> items;

    public OrderRoutingMatrix(
            List<LocationInfo> availableLocations,
            List<RoutingItem> routingItems
    ) {
        this.locationSize = availableLocations.size();
        this.itemSize = routingItems.size();

        this.locations = availableLocations;
        this.items = routingItems;

        this.matrix = new boolean[locationSize][itemSize];
    }

    public void markCanFulfill(Integer locationId, int index) {
        var location = locations.stream().filter(l -> l.getId() == locationId).findFirst().orElseThrow();
        var locationIndex = locations.indexOf(location);
        matrix[locationIndex][index] = true;
    }

    public List<FulfillmentGroup> groups() {
        var groups = new ArrayList<FulfillmentGroup>();
        var locationIds = new ArrayList<Integer>();
        for (int i = 0; i < locationSize; i++) {
            if (checkFulfillAllItems(i)) {
                locationIds.add(locations.get(i).getId());
            }
        }
        if (!locationIds.isEmpty()) {
            groups.add(
                    FulfillmentGroup.builder()
                            .items(this.items)
                            .locationIds(locationIds)
                            .build()
            );
        }

        return groups;
    }

    private boolean checkFulfillAllItems(int locationIndex) {
        for (var itemIndex = 0; itemIndex < itemSize; itemIndex++) {
            if (!matrix[locationIndex][itemIndex]) {
                return false;
            }
        }
        return true;
    }
}
