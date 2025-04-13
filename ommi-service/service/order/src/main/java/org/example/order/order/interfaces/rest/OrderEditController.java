package org.example.order.order.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.order.order.application.model.orderedit.request.OrderEditRequest;
import org.example.order.order.application.model.orderedit.response.OrderEditResponse;
import org.example.order.order.application.service.orderedit.OrderEditReadService;
import org.example.order.order.application.service.orderedit.OrderEditWriteService;
import org.example.order.order.domain.edit.model.OrderEditId;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/order-edits")
public class OrderEditController {

    private final OrderEditWriteService orderEditWriteService;
    private final OrderEditReadService orderEditReadService;

    @PostMapping("/begin/{order_id}")
    public OrderEditResponse begin(@PathVariable("order_id") int orderId) {
        int storeId = 1;
        var orderEditId = this.orderEditWriteService.begin(storeId, orderId);
        return orderEditReadService.getBeginEditResponse(orderEditId);
    }

    @PostMapping("/{id}/add_variants")
    public OrderEditResponse addVariants(@PathVariable int id, @RequestBody @Valid OrderEditRequest.AddVariants request) {
        int storeId = 1;
        var editingId = new OrderEditId(storeId, id);
        var lineItems = this.orderEditWriteService.addVariants(editingId, request.getAddVariants());
        return null;
    }

    @PostMapping("/{id}/add_custom_item")
    public OrderEditResponse addCustomItem(@PathVariable int id, @RequestBody @Valid OrderEditRequest.AddCustomItem request) {
        int storeId = 1;
        var orderEditId = new OrderEditId(storeId, id);
        var lineItemId = this.orderEditWriteService.addCustomItem(orderEditId, request);
        return null;
    }

    @PostMapping("/{id}/set_item_quantity")
    public OrderEditResponse setLineItemQuantity(
            @PathVariable int id,
            @RequestBody @Valid OrderEditRequest.SetItemQuantity request
    ) {
        int storeId = 1;
        var orderEditId = new OrderEditId(storeId, id);
        var lineItemId = this.orderEditWriteService.updateItemQuantity(orderEditId, request);
        return null;
    }

    @PostMapping("/{id}/set_item_discount")
    public OrderEditResponse setLineItemDiscount(
            @PathVariable int id,
            @RequestBody @Valid OrderEditRequest.SetItemDiscount request
    ) {
        int storeId = 1;
        var orderEditId = new OrderEditId(storeId, id);
        var lineItemId = this.orderEditWriteService.setItemDiscount(orderEditId, request);
        return null;
    }

    @PostMapping("/{id}/commit")
    public OrderEditResponse commit(@PathVariable int id,
                                    @RequestBody OrderEditRequest.Commit request) {

        int storeId = 1;
        var orderEditId = new OrderEditId(storeId, id);
        var orderId = orderEditWriteService.commit(orderEditId, request);
        return null;
    }
}
