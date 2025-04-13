package org.example.order.order.application.model.combination.response;

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
public class CombinationLineItemResponse {
    private Integer variantId;
    private Integer productId;

    private String token;
    private String title;
    private String variantTitle;
    private String unit;
    private String itemUnit;
    private String sku;

    private boolean taxable;
    private boolean requireShipping;

    private Integer grams;
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal linePrice = BigDecimal.ZERO;

    @Builder.Default
    private List<CombinationLineItemComponent> components = new ArrayList<>();

    @Builder.Default
    private List<ComboPacksizeDioscuntAllocationResponse> discountAllocations = new ArrayList<>();

    @Builder.Default
    private List<ComboPacksizeTaxLineResponse> taxLines = new ArrayList<>();

    private VariantType type;
}
