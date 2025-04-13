package org.example.product.product.domain.inventory.repository;

import org.example.product.product.domain.inventory.model.InventoryTrackingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTrackingRepository extends JpaRepository<InventoryTrackingRequest, Integer> {
    boolean existsByStoreIdAndIdempotencyKey(int storeId, String idempotencyKey);
}
