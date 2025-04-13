package org.example.product.product.domain.inventory.repository;

import org.example.product.product.domain.inventory.model.InventoryLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryLevelRepository extends JpaRepository<InventoryLevel, Integer> {

    List<InventoryLevel> getByStoreIdAndInventoryItemIdIn(int storeId, List<Integer> inventoryItemIds);

}