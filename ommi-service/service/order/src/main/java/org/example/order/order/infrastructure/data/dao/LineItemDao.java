package org.example.order.order.infrastructure.data.dao;

import java.util.List;

public interface LineItemDao {
    List<LineItemDto> getByOrderId(int storeId, int orderId);
}
