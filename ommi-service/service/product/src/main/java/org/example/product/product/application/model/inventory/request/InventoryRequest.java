package org.example.product.product.application.model.inventory.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class InventoryRequest {
    private String idempotencyKey;

    @Size(min = 1)
    private List<@Valid AdjustmentRequest> adjustments;

    private InventoryAdjustmentBehavior behavior;
}
