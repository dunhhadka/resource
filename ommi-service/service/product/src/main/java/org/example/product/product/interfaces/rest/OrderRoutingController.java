package org.example.product.product.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.product.product.application.model.product.OrderRoutingRequest;
import org.example.product.product.application.model.product.OrderRoutingResultResponse;
import org.example.product.product.application.service.routing.OrderRoutingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/order_routing")
public class OrderRoutingController {

    private final OrderRoutingService orderRoutingService;

    @PostMapping
    public List<OrderRoutingResultResponse> processRouting(@RequestBody @Valid OrderRoutingRequest orderRoutingRequest) {
        int storeId = 1;
        return orderRoutingService.process(storeId, orderRoutingRequest);
    }

}
