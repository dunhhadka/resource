package org.example.order.order.infrastructure.data.dao;

import java.util.List;

public interface OrderEditLineItemDao {
    List<OrderEditLineItemDto> getByOrderEditId(int storeId, int orderEditId);
}
