package org.example.order.order.domain.draftorder.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.utils.TaxLineUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

@Getter
public class DraftLineItem {
    @JsonUnwrapped
    @Valid
    private DraftProductInfo productInfo;

    private List<@Valid DraftTaxLine> taxLines = new ArrayList<>();

    @JsonUnwrapped
    @Valid
    private DraftLineItemShipInfo shipInfo;

    @Valid
    private DraftAppliedDiscount appliedDiscount;

    private List<@Valid DraftProperty> properties;

    /**
     * Tổng giá trị lineItem đã trừ đi khuyến mãi
     * = total_original - lineItem.applied_discount
     */
    private BigDecimal discountedTotal;

    /**
     * Giá sau khi áp dụng khuyến mãi của 1 đơn vị sản phẩm trong lineItem
     * = lineItem.price - round(lineItem.applied_discount / lineItem.quantity)
     */
    private BigDecimal discountedUnitPrice;

    /**
     * Tỉ lệ phân bổ = discounted_total/sum(discounted_total)
     */
    private BigDecimal allocationRatio;

    /**
     * Giá trị khuyến mãi đơn hàng/lineItem
     */
    private BigDecimal discountOrder;

    private List<@Valid DraftLineItemComponent> components;

    private List<DraftDiscountAllocation> discountAllocations;

    private boolean custom;

    public DraftLineItem(
            DraftProductInfo productInfo,
            DraftLineItemShipInfo shipInfo,
            DraftAppliedDiscount appliedDiscount,
            List<DraftProperty> properties
    ) {
        this.productInfo = productInfo;
        this.shipInfo = shipInfo;
        this.appliedDiscount = appliedDiscount;
        this.properties = properties;
    }

    public void setDiscountAllocations(List<DraftDiscountAllocation> discountAllocations) {
        if (CollectionUtils.isEmpty(discountAllocations)) return;
        this.discountAllocations = discountAllocations;
    }

    public BigDecimal calculateDiscountedTotal() {
        var originalLineTotalPrice = getOriginalLineTotalPrice();
        if (this.appliedDiscount == null) return originalLineTotalPrice;
        return originalLineTotalPrice.subtract(appliedDiscount.getAmount());
    }

    public BigDecimal getOriginalLineTotalPrice() {
        if (this.productInfo.getPrice().signum() == 0) return BigDecimal.ZERO;
        return this.productInfo.getPrice()
                .multiply(BigDecimal.valueOf(this.shipInfo.getQuantity()));
    }

    public void allocateDiscount(BigDecimal discountAllocatedAmount, boolean taxesIncluded, Currency currency) {
        this.discountOrder = discountAllocatedAmount;
        this.calculateTax(taxesIncluded, currency);
    }

    private void calculateTax(boolean taxesIncluded, Currency currency) {
        if (CollectionUtils.isEmpty(this.taxLines)) return;

        BigDecimal discountPrice = this.appliedDiscount != null ? this.appliedDiscount.getAmount() : BigDecimal.ZERO;
        BigDecimal currentPrice = this.getOriginalLineTotalPrice()
                .subtract(discountPrice)
                .subtract(this.discountOrder);
        this.taxLines.forEach(taxLine -> taxLine.setPrice(TaxLineUtils.calculateTaxPrice(taxLine.getRate(), currentPrice, currency, taxesIncluded)));
    }

    public void setTaxLines(List<DraftTaxLine> taxLines, boolean taxesIncluded, Currency currency) {
        if (CollectionUtils.isEmpty(taxLines)) return;
        this.taxLines = taxLines;
        this.calculateTax(taxesIncluded, currency);
    }

    public void removeTaxes() {
        this.taxLines = new ArrayList<>();
    }

    public void setMergedTaxLines(List<DraftTaxLine> taxLines) {
        this.taxLines = taxLines;
    }

    public void removeAllDiscountAllocations() {
        if (CollectionUtils.isEmpty(this.discountAllocations))
            return;
    }

    public void setComponents(List<DraftLineItemComponent> components) {
        if (CollectionUtils.isEmpty(components)) return;
        this.components = components;
    }
}
