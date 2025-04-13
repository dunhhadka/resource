package org.example.order.order.domain.fulfillmentorder.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrder;
import org.example.order.order.domain.fulfillmentorder.model.FulfillmentOrderId;
import org.example.order.order.domain.order.model.OrderId;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaFulfillmentOrderRepository implements FulfillmentOrderRepository {
    private final EntityManager entityManager;

    @Override
    public void save(FulfillmentOrder fulfillmentOrder) {
        var isNew = fulfillmentOrder.isNew();
        if (isNew) {
            entityManager.persist(fulfillmentOrder);
        } else {
            entityManager.merge(fulfillmentOrder);
        }
        entityManager.flush();
    }

    @Override
    public List<FulfillmentOrder> findByIds(List<FulfillmentOrderId> fulfillmentOrderIds) {
        return entityManager.createQuery("SELECT fo FROM FulfillmentOrder fo WHERE fo.id IN (:ids)", FulfillmentOrder.class)
                .setParameter("ids", fulfillmentOrderIds)
                .getResultList();
    }

    @Override
    public List<FulfillmentOrder> findByOrderId(OrderId orderId) {
        return entityManager.createQuery(
                        "SELECT fo FROM  FulfillmentOrder fo " +
                                "WHERE fo.id.storeId = :storeId AND fo.orderId = :orderId", FulfillmentOrder.class)
                .setParameter("storeId", orderId.getStoreId())
                .setParameter("orderId", orderId.getId())
                .getResultList();
    }
}
