package org.example.order.order.domain.transaction.persistence;

import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.transaction.model.OrderTransaction;

import java.util.List;

public interface TransactionRepository {
    void save(OrderTransaction orderTransaction);

    List<OrderTransaction> findByPaymentIds(int storeId, List<Integer> paymentIds);

    List<OrderTransaction> findByOrderId(OrderId id);
}
