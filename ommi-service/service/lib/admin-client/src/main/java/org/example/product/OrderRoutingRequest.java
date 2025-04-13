package org.example.product;

import java.math.BigDecimal;
import java.util.List;

public class OrderRoutingRequest {
    private String behavior;
    private List<OrderRoutingItemRequest> items;
    private ShippingAddress shippingAddress;


    public static class OrderRoutingItemRequest {
        private Integer variantId;
        private BigDecimal quantity;
        private boolean requireShipping;

        public Integer getVariantId() {
            return variantId;
        }

        public void setVariantId(Integer variantId) {
            this.variantId = variantId;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public boolean isRequireShipping() {
            return requireShipping;
        }

        public void setRequireShipping(boolean requireShipping) {
            this.requireShipping = requireShipping;
        }
    }

    public static class ShippingAddress {

    }

    public String getBehavior() {
        return behavior;
    }

    public void setBehavior(String behavior) {
        this.behavior = behavior;
    }

    public List<OrderRoutingItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderRoutingItemRequest> items) {
        this.items = items;
    }

    public ShippingAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(ShippingAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }
}
