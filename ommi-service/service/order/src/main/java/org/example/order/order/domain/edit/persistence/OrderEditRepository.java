package org.example.order.order.domain.edit.persistence;

import org.example.order.order.domain.edit.model.OrderEdit;
import org.example.order.order.domain.edit.model.OrderEditId;

public interface OrderEditRepository {
    void save(OrderEdit orderEdit);

    OrderEdit findById(OrderEditId orderEditId);
}
