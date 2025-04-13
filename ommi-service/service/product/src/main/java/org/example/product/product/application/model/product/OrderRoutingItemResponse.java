package org.example.product.product.application.model.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRoutingItemResponse {
    private int index;
    private int inventoryItemId;

    public OrderRoutingItemResponse(Integer itemIndex) {
        this.index = itemIndex;
    }
}
