package org.example.order.order.domain.draftorder.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Builder
@Getter
public class DraftTaxLine {
    @Size(max = 100)
    private String title;

    @Setter
    private BigDecimal price;

    @Max(1)
    @Min(0)
    private BigDecimal rate;

    @Max(100)
    @Min(0)
    private BigDecimal ratePercentage;

    public DraftTaxLine merge(DraftTaxLine taxLine) {
        this.price = this.price.add(taxLine.getPrice());
        return this;
    }
}
