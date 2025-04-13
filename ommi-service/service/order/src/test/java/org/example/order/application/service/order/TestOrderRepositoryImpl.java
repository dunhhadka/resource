package org.example.order.application.service.order;

import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.OrderId;
import org.example.order.order.domain.order.persistence.OrderRepository;

public class TestOrderRepositoryImpl implements OrderRepository {
    @Override
    public void save(Order order) {

    }

    @Override
    public Order findById(OrderId orderId) {
        return null;
    }
}
