package org.example.product.product.application.model.product;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRoutingResultResponse {
    private int locationId;
    private List<OrderRoutingItemResponse> items;
}
