package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;

import java.util.List;

public interface ProductDao {
    List<ProductDto> findProductByListId(int storeId, List<Integer> productIds);

    List<VariantDto> findVariantByListId(int storeId, List<Integer> variantIds);
}
