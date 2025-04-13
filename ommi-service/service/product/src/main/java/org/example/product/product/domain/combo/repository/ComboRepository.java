package org.example.product.product.domain.combo.repository;

import org.example.product.product.domain.combo.model.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComboRepository extends JpaRepository<Combo, Integer> {

    @Query(value = "SELECT c FROM Combo c WHERE c.storeId = :storeId AND c.variantId IN (SELECT cItem.variantId FROM c.comboItems cItem)")
    List<Combo> getByStoreIdAndSubVariantId(@Param("storeId") int storeId, @Param("variantId") int variantId);
}
