package org.example.order.order.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.refund.request.RefundRequest;
import org.example.order.order.application.model.refund.response.RefundResponse;
import org.example.order.order.application.service.order.OrderWriteService;
import org.example.order.order.domain.order.model.OrderId;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/orders/{order_id}/refunds")
public class OrderRefundController {

    private final OrderWriteService orderWriteService;

    @PostMapping
    public RefundResponse createRefund(@Valid @RequestBody RefundRequest request,
                                       @PathVariable("order_id") int orderId
    ) {
        int storeId = 1;
        var refundId = orderWriteService.createRefund(new OrderId(storeId, orderId), request);
        return null;
    }
}
