package org.example.product.product.interfaces.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.product.product.application.model.inventory.request.InventoryRequest;
import org.example.product.product.application.service.inventory.InventoryLevelWriteService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/inventory_adjustments")
public class InventoryAdjustmentController {

    private final InventoryLevelWriteService inventoryLevelWriteService;

    @PostMapping("/commit_inventory")
    public void commitInventory(@RequestBody @Valid InventoryRequest request) {
        int storeId = 1;
        inventoryLevelWriteService.commitInventory(storeId, request);
    }
}
