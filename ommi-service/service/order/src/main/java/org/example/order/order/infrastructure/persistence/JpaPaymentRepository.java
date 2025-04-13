package org.example.order.order.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.payment.model.Payment;
import org.example.order.order.domain.payment.persistence.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaPaymentRepository implements PaymentRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public List<Payment> findAvailableByCheckoutToken(int storeId, String checkoutToken) {
        return this.entityManager
                .createQuery("SELECT p FROM Payment p " +
                        "WHERE p.id.storeId = :storeId " +
                        "AND p.identifyMutation.checkoutToken = :checkoutToken " +
                        "AND p.status IN (:status)", Payment.class)
                .setParameter("storeId", storeId)
                .setParameter("checkoutToken", checkoutToken)
                .setParameter("status", List.of(Payment.PaymentStatus.success, Payment.PaymentStatus.pending))
                .getResultList();
    }
}
