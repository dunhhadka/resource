package org.example.product;

import java.util.List;

public class OrderRouting {
    private int locationId;
    private List<OrderRoutingItem> items;

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public List<OrderRoutingItem> getItems() {
        return items;
    }

    public void setItems(List<OrderRoutingItem> items) {
        this.items = items;
    }
}
