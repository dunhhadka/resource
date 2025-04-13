package org.example.order.order.infrastructure.persistence;

import org.example.order.order.domain.draftorder.model.DraftOrderNumber;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftOrderNumberRepository extends JpaRepository<DraftOrderNumber, Integer> {
    DraftOrderNumber findFirstByStoreId(int storeId);
}
