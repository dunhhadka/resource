package org.example.product.product.application.model.combo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ComboRequest {
    @Min(1)
    private int variantId;
    @Size(max = 20)
    @NotEmpty
    private List<@Valid ComboItemRequest> comboItems;

    private BigDecimal price;
}
