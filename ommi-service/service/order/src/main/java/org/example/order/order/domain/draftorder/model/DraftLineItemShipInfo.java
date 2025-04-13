package org.example.order.order.domain.draftorder.model;

import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DraftLineItemShipInfo {
    @Min(1)
    private int quantity;

    @Min(0)
    private int grams;

    private boolean requireShipping;
}
