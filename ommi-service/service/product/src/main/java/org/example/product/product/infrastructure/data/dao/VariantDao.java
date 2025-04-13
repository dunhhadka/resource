package org.example.product.product.infrastructure.data.dao;

import org.example.product.product.infrastructure.data.dto.VariantDto;

import java.util.List;

public interface VariantDao {

    VariantDto getByVariantId(int storeId, int variantId);

    List<VariantDto> getByVariantIds(int storeId, List<Integer> variantIds);
}
