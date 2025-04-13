package org.example.order.order.domain.draftorder.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
public class DraftOrderShippingLine {
    @Size(max = 150)
    @NotBlank
    private String title;
    @Size(max = 255)
    private String alias;

    @Builder.Default
    private List<DraftTaxLine> taxLines = new ArrayList<>();

    @Min(0)
    @NotNull
    private BigDecimal price;

    private boolean custom;

    @Size(max = 50)
    private String source;

    public void addTax(DraftTaxLine taxLine) {
        if (this.taxLines == null) taxLines = new ArrayList<>();
        this.taxLines.add(taxLine);
    }

    public void removeTax() {
        if (CollectionUtils.isEmpty(this.taxLines)) return;
        this.taxLines = new ArrayList<>();
    }
}
