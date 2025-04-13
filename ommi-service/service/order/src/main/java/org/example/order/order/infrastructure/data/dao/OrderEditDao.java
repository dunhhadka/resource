package org.example.order.order.infrastructure.data.dao;

public interface OrderEditDao {
    OrderEditDto getById(int storeId, int id);
}
