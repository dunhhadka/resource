package org.example.order.order.application.model.draftorder.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@Jacksonized
public class DraftLineItemRequest {
    private Integer variantId;
    private Integer productId;
    private @Size(max = 320) String title;
    private @Size(max = 500) String variantTitle;
    private @Min(0) BigDecimal price;
    private @Min(1) int quantity;
    private @Min(0) int grams;
    private boolean requireShipping;
    private boolean taxable;
    private String sku;
    private String vendor;
    private String fulfillmentService;

    private List<DraftPropertyRequest> properties;

    private DraftAppliedDiscountRequest appliedDiscount;

    public boolean isCustom() {
        return variantId == null;
    }
}
