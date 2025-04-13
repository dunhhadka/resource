package org.example.order.order.infrastructure.data.dao;

import java.util.List;

public interface OrderEditDiscountApplicationDao {
    List<OrderEditDiscountApplicationDto> getByOrderEditId(int storeId, int orderEditId);
}
