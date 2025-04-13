package org.example.order.order.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.example.order.order.domain.draftorder.model.DraftOrderNumber;
import org.example.order.order.domain.draftorder.persistence.DraftOrderNumberGenerator;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DraftOrderNumberGeneratorImpl implements DraftOrderNumberGenerator {
    private final DraftOrderNumberRepository repository;

    @Override
    public int generateDraftNumber(int storeId) {
        var draftNumber = repository.findFirstByStoreId(storeId);
        if (draftNumber == null) {
            draftNumber = DraftOrderNumber.builder()
                    .storeId(storeId)
                    .currentOrderNumber(1)
                    .build();
            repository.save(draftNumber);
            return 1;
        }
        draftNumber.update();
        repository.save(draftNumber);
        return draftNumber.getCurrentOrderNumber();
    }
}
