package org.example.product.product.application.model.combo;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ComboItemRequest {
    @Min(1)
    private int variantId;

    @Min(1)
    private int quantity;
}
