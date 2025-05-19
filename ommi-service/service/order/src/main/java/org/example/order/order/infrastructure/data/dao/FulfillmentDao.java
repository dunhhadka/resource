package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.FulfillmentDto;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface FulfillmentDao {
    CompletableFuture<List<FulfillmentDto>> getFulfillmentByStoreIdsAndOrderIdsAsync(List<Integer> storeIds, List<Integer> orderIds);

    List<FulfillmentDto> getFulfillmentByStoreIdsAndOrderIds(Set<Integer> storeIds, Set<Integer> orderIds);
}
