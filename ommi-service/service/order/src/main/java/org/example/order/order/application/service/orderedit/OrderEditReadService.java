package org.example.order.order.application.service.orderedit;

import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderedit.response.CalculatedOrder;
import org.example.order.order.application.model.orderedit.response.OrderEditResponse;
import org.example.order.order.domain.edit.model.OrderEditId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderEditReadService {

    private final OrderEditCalculatorService orderEditCalculatorService;

    public OrderEditResponse getBeginEditResponse(OrderEditId orderEditId) {
        var calculatedOrder = calculateOrder(orderEditId);

        return OrderEditResponse.builder().calculatedOrder(calculatedOrder).build();
    }

    private CalculatedOrder calculateOrder(OrderEditId orderEditId) {
        return orderEditCalculatorService.calculateOrder(orderEditId);
    }
}
