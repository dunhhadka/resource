package org.example.order.order.infrastructure.data.dao;

import java.util.List;

public interface OrderEditDiscountAllocationDao {
    List<OrderEditDiscountAllocationDto> getByOrderEditId(int storeId, int orderEditId);
}
