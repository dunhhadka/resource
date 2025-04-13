package org.example.order.order.application.model.order.response;

import lombok.Builder;
import lombok.Getter;
import org.example.order.order.domain.order.model.Order;

import java.util.List;

@Getter
@Builder
public class OrderPaymentResult {
    private final boolean isFromCheckout;
    private final String checkoutToken;
    private final List<Integer> paymentIds;
    private final List<Order.TransactionInput> transactions;
}
