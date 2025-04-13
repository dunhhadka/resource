package org.example.order.order.application.model.orderedit.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OrderEditResponse {
    private CalculatedOrder calculatedOrder;

    private List<CalculatedLineItem> calculatedLineItems;

    private OrderStagedChangeModel stagedChange;
}
