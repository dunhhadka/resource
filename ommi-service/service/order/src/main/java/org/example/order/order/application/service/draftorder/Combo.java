package org.example.order.order.application.service.draftorder;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class Combo {
    private int productId;
    private int variantId;
    private String unit;
    private String itemUnit;
    private BigDecimal price;
    private List<ComboItem> comboItems;
}
