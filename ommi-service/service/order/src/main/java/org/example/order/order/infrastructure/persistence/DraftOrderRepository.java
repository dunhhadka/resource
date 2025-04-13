package org.example.order.order.infrastructure.persistence;

import org.example.order.order.domain.draftorder.model.DraftOrder;

public interface DraftOrderRepository {
    void save(DraftOrder draftOrder);
}
