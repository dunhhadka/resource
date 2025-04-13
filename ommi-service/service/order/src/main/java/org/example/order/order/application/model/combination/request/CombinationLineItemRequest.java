package org.example.order.order.application.model.combination.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.example.order.order.domain.draftorder.model.VariantType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinationLineItemRequest {
    private Integer variantId;
    private Integer productId;

    private Integer inventoryItemId;
    private String inventoryPolicy;
    private String inventoryManagement;

    private BigDecimal price;

    private BigDecimal linePrice;

    @NotBlank
    private String title;
    private String variantTitle;
    private String sku;

    private boolean taxable;

    private String vendor;
    private String unit;
    private String itemUnit;

    private VariantType type;

    private @Min(0) BigDecimal quantity;
    private int grams;

    private boolean requireShipping;

    @Builder.Default
    private List<LineItemComponent> components = new ArrayList<>();


    @Builder.Default
    private List<@Valid ComboPacksizeDiscountAllocations> discountAllocations = new ArrayList<>();

    @Builder.Default
    private List<@Valid ComboPacksizeTaxLine> taxLines = new ArrayList<>();

    private List<CustomAttributeRequest> attributes;

    public boolean isCustom() {
        return this.variantId == null;
    }
}