package org.example.order.order.domain.draftorder.persistence;

public interface DraftOrderNumberGenerator {
    int generateDraftNumber(int storeId);
}
