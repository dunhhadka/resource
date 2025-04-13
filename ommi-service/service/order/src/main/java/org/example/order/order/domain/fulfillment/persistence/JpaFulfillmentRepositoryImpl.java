package org.example.order.order.domain.fulfillment.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.fulfillment.model.Fulfillment;
import org.example.order.order.domain.fulfillment.model.FulfillmentId;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JpaFulfillmentRepositoryImpl implements FulfillmentRepository {

    private final EntityManager entityManager;

    @Override
    public void save(Fulfillment fulfillment) {
        var isNew = fulfillment.isNew();
        if (isNew) {
            entityManager.persist(fulfillment);
        } else {
            entityManager.merge(fulfillment);
        }
        entityManager.flush();
    }

    @Override
    public List<Fulfillment> getByIds(List<FulfillmentId> fulfillmentIds) {
        return entityManager.createQuery("SELECT ff FROM Fulfillment ff WHERE ff.id IN (:ids)", Fulfillment.class)
                .setParameter("ids", fulfillmentIds)
                .getResultList();
    }
}
