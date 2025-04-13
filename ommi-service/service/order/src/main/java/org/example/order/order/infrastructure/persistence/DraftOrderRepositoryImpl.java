package org.example.order.order.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.draftorder.model.DraftOrder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DraftOrderRepositoryImpl implements DraftOrderRepository {

    private final EntityManager entityManager;

    @Override
    public void save(DraftOrder draftOrder) {
        var isNew = draftOrder.isNew();
        if (isNew) entityManager.persist(draftOrder);
        else entityManager.merge(draftOrder);
    }
}
