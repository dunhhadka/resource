package org.example.order.order.infrastructure.data.dao;

import java.util.List;

public interface OrderEditTaxLineDao {
    List<OrderEditTaxLineDto> getByOrderEditId(int storeId, int orderEditId);
}
