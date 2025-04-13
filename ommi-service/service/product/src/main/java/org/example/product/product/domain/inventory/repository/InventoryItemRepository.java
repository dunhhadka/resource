package org.example.product.product.domain.inventory.repository;

import org.example.product.product.domain.inventory.model.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Integer> {

    List<InventoryItem> getByStoreIdAndVariantIdIn(int storeId, List<Integer> variantIds);

    List<InventoryItem> getByStoreIdAndIdIn(int storeId, List<Integer> ids);
}
