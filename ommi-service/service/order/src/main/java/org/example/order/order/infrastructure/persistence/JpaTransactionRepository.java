package org.example.order.order.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.persistence.TransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaTransactionRepository implements TransactionRepository {

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public void save(OrderTransaction orderTransaction) {
        var isNew = orderTransaction.isNew();
        if (isNew) {
            entityManager.persist(orderTransaction);
        } else {
            entityManager.merge(orderTransaction);
        }
        entityManager.flush();
    }

    @Override
    public List<OrderTransaction> findByPaymentIds(int storeId, List<Integer> paymentIds) {
        return entityManager
                .createQuery(
                        "SELECT o FROM OrderTransaction o " +
                                "WHERE " +
                                "o.id.storeId = :storeId " +
                                "AND o.paymentInfo.paymentId IN (:paymentIds)",
                        OrderTransaction.class)
                .setParameter("storeId", storeId)
                .setParameter("paymentIds", paymentIds)
                .getResultList();
    }

    @Override
    public List<OrderTransaction> findByOrderId(OrderId id) {
        return List.of();
    }
}
