package org.example.order.order.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.order.request.OrderCreateRequest;
import org.example.order.order.application.model.order.response.OrderResponse;
import org.example.order.order.application.service.order.OrderWriteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class OrderController {

    private final OrderWriteService orderWriteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@RequestBody @Valid OrderCreateRequest request) {
        int storeId = 1;
        var orderId = orderWriteService.createOrder(storeId, request);

        return null;
    }
}
