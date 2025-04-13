package org.example.product.product.application.model.product;

import lombok.Getter;
import lombok.Setter;
import org.example.product.product.application.service.routing.Behavior;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class OrderRoutingRequest {
    private List<OrderRoutingItemRequest> items;

    private ShippingAddress shippingAddress;

    private Behavior behavior;

    @Getter
    @Setter
    public static class OrderRoutingItemRequest {
        private Integer variantId;
        private BigDecimal quantity;
        private boolean requireShipping;
    }


    public record ShippingAddress(int provinceId) {
    }
}
