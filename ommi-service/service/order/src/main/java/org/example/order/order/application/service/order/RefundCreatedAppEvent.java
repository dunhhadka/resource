package org.example.order.order.application.service.order;

import lombok.Getter;
import org.example.order.order.application.model.order.request.OrderTransactionCreateRequest;
import org.example.order.order.domain.order.model.Order;

import java.util.List;

@Getter
public class RefundCreatedAppEvent {
    private final int storeId;
    private final int orderId;

    private final Order order;

    private final List<RestockLineItem> restockLineItems;

    private final List<OrderTransactionCreateRequest> transactions;

    public RefundCreatedAppEvent(
            Order order,
            List<RestockLineItem> restockLineItems,
            List<OrderTransactionCreateRequest> transactions
    ) {
        this.storeId = order.getId().getStoreId();
        this.orderId = order.getId().getId();
        this.order = order;
        this.restockLineItems = restockLineItems;
        this.transactions = transactions;
    }


    public record RestockLineItem(
            int locationId,
            int lineItemId,
            int quantity,
            boolean fufilled,
            boolean restocked
    ) {
    }
}
