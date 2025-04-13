package org.example.order.order.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.AdminClient;
import org.example.order.order.application.model.draftorder.request.DraftOrderCreateRequest;
import org.example.order.order.application.model.draftorder.response.DraftOrderResponse;
import org.example.order.order.application.service.draftorder.DraftOrderWriteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/draft_orders")
public class DraftOrderController {

    private final AdminClient adminClient;
    private final DraftOrderWriteService draftOrderWriteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DraftOrderResponse create(@RequestBody @Valid DraftOrderCreateRequest request) {
        var message = adminClient.productTest();
        int storeId = 1;
        var draftOrderId = draftOrderWriteService.createDraftOrder(storeId, request);
        return null;
    }
}
