package org.example.order.order.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.edit.model.OrderEdit;
import org.example.order.order.domain.edit.model.OrderEditId;
import org.example.order.order.domain.edit.persistence.OrderEditRepository;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaOrderEditRepository implements OrderEditRepository {

    private final EntityManager entityManager;

    @Override
    public void save(OrderEdit orderEdit) {
        var isNew = orderEdit.isNew();
        if (isNew) {
            entityManager.persist(orderEdit);
        } else {
            entityManager.merge(orderEdit);
        }
        entityManager.flush();
    }

    @Override
    public OrderEdit findById(OrderEditId orderEditId) {
        return this.entityManager.find(OrderEdit.class, orderEditId);
    }
}
