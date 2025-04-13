package org.example.product.product.domain.product.repository;

import org.springframework.stereotype.Component;

import java.util.Deque;

@Component
public class SQLProductIdGenerator implements ProductIdGenerator {

    @Override
    public Deque<Integer> generateImageIds(long validImageCount) {
        return null;
    }

    @Override
    public int generateVariantId() {
        return 0;
    }

    @Override
    public int generateInventoryItemId() {
        return 0;
    }

    @Override
    public int generateProductId() {
        return 0;
    }

    @Override
    public Deque<Integer> generateVariantIds(int size) {
        return null;
    }

    @Override
    public Deque<Integer> generateInventoryItemIds(int size) {
        return null;
    }
}
