package org.example.product.product.domain.product.repository;

import java.util.Deque;

public interface ProductIdGenerator {

    Deque<Integer> generateImageIds(long validImageCount);

    int generateVariantId();

    int generateInventoryItemId();

    int generateProductId();

    Deque<Integer> generateVariantIds(int size);

    Deque<Integer> generateInventoryItemIds(int size);
}
