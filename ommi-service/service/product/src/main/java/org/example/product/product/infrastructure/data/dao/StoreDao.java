package org.example.product.product.infrastructure.data.dao;

import org.example.product.product.infrastructure.data.dto.StoreDto;

public interface StoreDao {
    default StoreDto getStoreById(int storeId) {
        return StoreDto.builder()
                .id(storeId)
                .maxProduct(10000)
                .name("Ommi Store")
                .build();
    }
}
