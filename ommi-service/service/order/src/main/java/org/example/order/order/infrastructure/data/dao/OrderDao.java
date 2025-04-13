package org.example.order.order.infrastructure.data.dao;

import org.example.order.order.infrastructure.data.dto.OrderDto;

public interface OrderDao {

    OrderDto getByReference(int storeId, String reference);

    OrderDto getById(int storeId, int id);
}
