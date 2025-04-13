package org.example.order.order.infrastructure.persistence;

import org.example.order.order.domain.draftorder.persistence.DraftOrderIdGenerator;
import org.springframework.stereotype.Repository;

@Repository
public class DBDraftOrderIdGenerator implements DraftOrderIdGenerator {

    @Override
    public int generateDraftOrderId() {
        return 0;
    }
}
