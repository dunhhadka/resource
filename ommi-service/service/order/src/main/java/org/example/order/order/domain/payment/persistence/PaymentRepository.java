package org.example.order.order.domain.payment.persistence;

import org.example.order.order.domain.payment.model.Payment;

import java.util.List;

public interface PaymentRepository {
    List<Payment> findAvailableByCheckoutToken(int storeId, String checkoutToken);
}
