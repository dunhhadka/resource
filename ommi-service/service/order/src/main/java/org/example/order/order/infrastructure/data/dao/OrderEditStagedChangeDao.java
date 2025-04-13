package org.example.order.order.infrastructure.data.dao;

import java.util.List;

public interface OrderEditStagedChangeDao {
    List<OrderEditStagedChangeDto> getByOrderEditId(int storeId, int orderEditId);
}
