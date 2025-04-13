package org.example.order.order.domain.order.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaOrderRepository implements OrderRepository {

    private final EntityManager entityManager;

    @Override
    public void save(Order order) {
        var isNew = order.isNew();
        if (isNew)
            entityManager.persist(order);
        else
            entityManager.merge(order);

        entityManager.flush();
    }

    @Override
    public Order findById(OrderId orderId) {
        return this.entityManager.find(Order.class, orderId);
    }
}
