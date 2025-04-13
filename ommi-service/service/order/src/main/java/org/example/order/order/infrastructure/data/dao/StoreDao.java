package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.StoreDto;

public interface StoreDao {
    default StoreDto getStoreById(int storeId) {
        return StoreDto.builder()
                .build();
    }
}
