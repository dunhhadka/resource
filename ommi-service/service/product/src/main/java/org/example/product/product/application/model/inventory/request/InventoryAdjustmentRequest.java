package org.example.product.product.application.model.inventory.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InventoryAdjustmentRequest {
    @NotBlank
    private String idempotencyKey;

    private List<InventoryAdjustmentTransactionRequest> transactions;
}
