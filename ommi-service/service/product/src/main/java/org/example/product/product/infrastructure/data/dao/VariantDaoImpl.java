package org.example.product.product.infrastructure.data.dao;

import org.example.product.product.infrastructure.data.dto.VariantDto;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VariantDaoImpl implements VariantDao {
    @Override
    public VariantDto getByVariantId(int storeId, int variantId) {
        return null;
    }

    @Override
    public List<VariantDto> getByVariantIds(int storeId, List<Integer> variantIds) {
        return null;
    }
}
