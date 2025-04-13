package org.example.order.order.application.model.combination.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.application.model.combination.request.ComboPacksizeDiscountAllocations;
import org.example.order.order.domain.draftorder.model.VariantType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Builder
public class CombinationLineItemComponent {
    private int variantId;
    private int productId;
    private Integer inventoryItemId;
    private String sku;
    private String title;
    private String variantTitle;
    private String vendor;
    private String unit;
    private String inventoryManagement;
    private String inventoryPolicy;
    private int grams;
    private boolean requireShipping;
    private boolean taxable;
    private BigDecimal quantity;

    private BigDecimal baseQuantity;

    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    private BigDecimal linePrice;

    private BigDecimal subtotal;

    private BigDecimal remainder;

    private List<ComboPacksizeDioscuntAllocationResponse> discountAllocations;

    private ComboPacksizeDioscuntAllocationResponse discountRemainder;

    private List<ComboPacksizeTaxLineResponse> taxLines;

    private VariantType type;

    // Line có thể là số lẻ hay không
    private boolean canBeOdd;

    private boolean changed;

    public void setLinePrice(BigDecimal linePrice) {
        this.linePrice = linePrice;
        this.changed = true;
    }

    public void addDiscountAllocations(ComboPacksizeDioscuntAllocationResponse allocation) {
        if (this.discountAllocations == null) this.discountAllocations = new ArrayList<>();
        this.discountAllocations.add(allocation);
    }

    public void addTaxLine(ComboPacksizeTaxLineResponse taxLine) {
        if (this.taxLines == null) this.taxLines = new ArrayList<>();
        this.taxLines.add(taxLine);
    }
}
