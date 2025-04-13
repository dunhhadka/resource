package org.example.order.order.domain.draftorder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.model.draftorder.TaxSetting;
import org.example.order.order.application.model.draftorder.TaxSettingValue;
import org.example.order.order.application.service.draftorder.TaxHelper;
import org.example.order.order.application.utils.BigDecimals;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.TaxLineUtils;
import org.example.order.order.domain.draftorder.persistence.DraftOrderNumberGenerator;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Entity
@Table(name = "draft_orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftOrder extends AggregateRoot<DraftOrder> {

    @EmbeddedId
    @JsonUnwrapped
    @AttributeOverride(name = "id", column = @Column(name = "id"))
    @AttributeOverride(name = "storeId", column = @Column(name = "storeId"))
    private DraftOrderId id;

    @NotBlank
    @Size(max = 505)
    private String name;

    @Min(0)
    private Integer copyOrderId;

    private Integer userId;

    private Instant modifiedOn;
    private Instant createdOn;

    @Valid
    @JsonUnwrapped
    @Embedded
    private DraftOrderPricingInfo pricingInfo;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private Status status;

    private Instant completedOn;

    private Integer orderId;

    private Integer customerId;

    private boolean taxesIncluded;

    @Size(max = 36)
    private String requestId;

    @Setter
    @JsonUnwrapped
    @Valid
    @Embedded
    private DraftOrderInfo draftOrderInfo;

//    @ElementCollection(fetch = FetchType.EAGER)
//    @CollectionTable(name = "draft_order_tags", joinColumns = {
//            @JoinColumn(name = "draftOrderId", referencedColumnName = "id"),
//            @JoinColumn(name = "storeId", referencedColumnName = "storeId")
//    })
//    private List<@Valid DraftOrderTag> tags = new ArrayList<>();

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private DraftOrderAddress shippingAddress; // Địa chỉ nhân hàng

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private DraftOrderAddress billingAddress; // Địa chỉ thanh toán

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private DraftOrderShippingLine shippingLine;

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private DraftAppliedDiscount appliedDiscount;

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private List<DraftLineItem> lineItems = new ArrayList<>();

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private List<DraftProperty> noteAttributes;

    @Valid
    @Type(JsonType.class)
    @Column(columnDefinition = "nvarchar(max)")
    private List<DraftDiscountApplication> discountApplications;

    private BigDecimal grams;

    // cache do calculate price có thể sử dụng nhiều lần
    @Transient
    private TaxSetting taxSetting;

    @Transient
    @JsonIgnore
    private TaxHelper taxHelper;

    public DraftOrder(
            DraftOrderId id,
            DraftOrderNumberGenerator numberGenerator,
            Integer copyOrderId,
            Currency currency,
            Integer userId,
            String idempotencyKey,
            TaxHelper taxHelper) {
        this.id = id;
        this.name = "#" + numberGenerator.generateDraftNumber(id.getStoreId());
        this.copyOrderId = copyOrderId;
        this.userId = userId;
        this.draftOrderInfo = DraftOrderInfo.builder().currency(currency).build();
        if (StringUtils.isNotBlank(idempotencyKey)) {
            this.requestId = idempotencyKey;
        }
        this.status = Status.open;
        this.taxHelper = taxHelper;
    }

    public void setDiscountApplications(List<DraftDiscountApplication> discountApplications) {
        if (allowEdit() && CollectionUtils.isNotEmpty(discountApplications)) {
            this.discountApplications = discountApplications;
        }
    }

    private boolean allowEdit() {
        return this.status == Status.open;
    }

    public void setLineItems(List<DraftLineItem> lineItems) {
        if (allowEdit()) {
            this.lineItems = lineItems;
            this.grams = this.lineItems.stream()
                    .map(line -> line.getShipInfo().getGrams())
                    .filter(NumberUtils::isPositive)
                    .map(BigDecimal::valueOf)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            this.calculatePrice();
        }
    }

    private void calculatePrice() {
        if (this.pricingInfo == null) {
            this.pricingInfo = new DraftOrderPricingInfo();
        }
        if (CollectionUtils.isEmpty(this.lineItems)) {
            return;
        }

        var currency = this.draftOrderInfo.getCurrency();

        // Giá sau khi đã tính giảm giá
        var lineItemSubtotalPrice = this.getLineItemSubtotal();
        // Tổng giá gốc ban đầu chỉ tính line_items
        var lineItemOriginalPrice = this.getOriginalLinePrice();

        var taxSetting = this.getTaxSetting();
        this.taxesIncluded = taxSetting.isTaxIncluded();

        boolean shouldTax = shouldCalculateTax(this.draftOrderInfo, taxSetting);
        boolean taxShipping = taxSetting.isTaxShipping();

        var lineTaxDefaultValue = taxSetting.getTaxes().stream()
                .filter(tax -> tax.getType() == null || tax.getProductId() == null)
                .findFirst().orElse(TaxSettingValue.builder().build());
        var shippingTax = taxSetting.getTaxes().stream()
                .filter(tax -> tax.getType() != null && tax.getType() == TaxSettingValue.TaxType.shipping)
                .findFirst().orElse(lineTaxDefaultValue);

        if (this.shippingLine != null) {
            if (shouldTax && taxShipping) {
                this.shippingLine.addTax(TaxLineUtils.buildDraftTaxLine(shippingTax, this.shippingLine.getPrice(), currency, taxesIncluded));
            } else {
                shippingLine.removeTax();
            }
        }

        // discount
        if (this.appliedDiscount != null) {
            var discountAmount = switch (this.appliedDiscount.getValueType()) {
                case fixed_amount -> this.appliedDiscount.getValue();
                case percentage -> lineItemSubtotalPrice
                        .multiply(this.appliedDiscount.getAmount())
                        .divide(BigDecimals.ONE_HUND0RED, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
            };
            this.appliedDiscount.setAmount(discountAmount.min(lineItemSubtotalPrice));

            // phân bổ giá về các line
            BigDecimal allocatedAmount = BigDecimal.ZERO;
            int lastIndex = this.lineItems.size() - 1;
            for (int i = 0; i < this.lineItems.size(); i++) {
                var lineItem = this.lineItems.get(i);
                BigDecimal discountAllocatedAmount;
                if (i != lastIndex) {
                    discountAllocatedAmount = lineItem.getDiscountedTotal()
                            .multiply(discountAmount)
                            .divide(lineItemSubtotalPrice, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
                    allocatedAmount = allocatedAmount.add(discountAllocatedAmount);
                } else {
                    discountAllocatedAmount = discountAmount.subtract(allocatedAmount);
                }
                lineItem.allocateDiscount(discountAllocatedAmount, taxesIncluded, currency);
            }
        }

        // taxLine: phân bổ taxLine về lineItems
        if (shouldTax) {
            var taxMap = taxSetting.getTaxes().stream()
                    .collect(Collectors.toMap(
                            TaxSettingValue::getProductId,
                            Function.identity(),
                            (first, second) -> second));
            for (var lineItem : this.lineItems) {
                switch (lineItem.getProductInfo().getType()) {
                    case normal -> {
                        if (lineItem.getProductInfo().isTaxable()) {
                            var tax = taxMap.getOrDefault(lineItem.getProductInfo().getProductId(), lineTaxDefaultValue);
                            lineItem.setTaxLines(
                                    List.of(TaxLineUtils.buildDraftTaxLine(tax, lineItem.getDiscountedTotal(), currency, taxesIncluded)),
                                    taxesIncluded,
                                    currency
                            );
                        } else {
                            lineItem.removeTaxes();
                        }
                    }
                    case combo, packsize -> {
                        /**
                         * Đối với sản phẩm combo/packsize => tính toán thuế ở các thành phần sau đó tổng hợp lên line gốc
                         * */
                        Map<String, DraftTaxLine> mergedTaxLines = new HashMap<>();
                        var components = lineItem.getComponents();
                        for (var component : components) {
                            var taxValue = taxMap.get(component.getProductId());
                            var componentTaxLine = TaxLineUtils.buildDraftTaxLine(
                                    taxValue,
                                    component.getPrice(),
                                    currency,
                                    taxesIncluded
                            );
                            TaxLineUtils.mergeTaxLine(mergedTaxLines, componentTaxLine);
                        }
                        lineItem.setMergedTaxLines(mergedTaxLines.values().stream().toList());
                    }
                }
            }
        }

        var cartDiscountAmount = this.appliedDiscount != null ? this.appliedDiscount.getAmount() : BigDecimal.ZERO;
        var subtotalPrice = lineItemSubtotalPrice.subtract(cartDiscountAmount);
        var totalShippingPrice = getTotalShippingPrice();

        var totalTaxLine = this.lineItems.stream()
                .filter(line -> CollectionUtils.isNotEmpty(line.getTaxLines()))
                .flatMap(line -> line.getTaxLines().stream())
                .map(DraftTaxLine::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalTaxShip = this.shippingLine != null
                ? this.shippingLine.getTaxLines().stream().map(DraftTaxLine::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        var totalTaxPrice = totalTaxLine.add(totalTaxShip);

        var totalPrice = subtotalPrice.add(totalShippingPrice).add(!taxesIncluded ? totalTaxPrice : BigDecimal.ZERO);

        var totalDiscount = this.lineItems.stream()
                .filter(line -> line.getAppliedDiscount() != null)
                .map(line -> line.getAppliedDiscount().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.pricingInfo = this.pricingInfo.toBuilder()
                .subtotalPrice(subtotalPrice)
                .totalPrice(totalPrice)
                .lineItemSubtotalPrice(lineItemSubtotalPrice)
                .totalDiscounts(totalDiscount)
                .totalTax(totalTaxPrice)
                .totalShippingPrice(totalShippingPrice)
                .totalLineItemPrice(lineItemOriginalPrice)
                .build();
    }

    private BigDecimal getTotalShippingPrice() {
        return this.shippingLine == null ? BigDecimal.ZERO : this.shippingLine.getPrice();
    }

    private boolean shouldCalculateTax(DraftOrderInfo draftOrderInfo, TaxSetting taxSetting) {
        if (draftOrderInfo.getTaxExempt() != null) {
            return draftOrderInfo.getTaxExempt();
        }
        return taxSetting.getStatus() == TaxSetting.TaxStatus.active;
    }

    private TaxSetting getTaxSetting() {
        String countryCode = this.shippingAddress != null ? this.shippingAddress.getCountryCode() : null;

        if (countryCode == null) countryCode = "VN";
        else if (!"VN".equals(countryCode)) return TaxSetting.defaultTax();

        Set<Integer> productIds = new HashSet<>();
        for (var lineItem : this.lineItems) {
            if (lineItem.isCustom()) continue;
            if (lineItem.getProductInfo().getType() == VariantType.normal) {
                productIds.add(lineItem.getProductInfo().getProductId());
                continue;
            }
            var componentProductIds = lineItem.getComponents().stream().map(DraftLineItemComponent::getProductId).distinct().toList();
            productIds.addAll(componentProductIds);
        }
        if (lineItems.stream().anyMatch(line -> line.isCustom() && line.getProductInfo().isTaxable()))
            productIds.add(0);

        if (this.taxSetting != null
                && StringUtils.equals(this.taxSetting.getCountryCode(), countryCode)
                && CollectionUtils.isEqualCollection(productIds, taxSetting.getProductIds())) {
            return this.taxSetting;
        }

        return taxHelper.getTaxSetting(this.id.getStoreId(), countryCode, productIds, true);
    }

    private BigDecimal getOriginalLinePrice() {
        if (CollectionUtils.isEmpty(this.lineItems)) return BigDecimal.ZERO;
        return this.lineItems.stream()
                .map(DraftLineItem::getOriginalLineTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getLineItemSubtotal() {
        if (CollectionUtils.isEmpty(this.lineItems)) return BigDecimal.ZERO;
        return this.lineItems.stream()
                .map(DraftLineItem::calculateDiscountedTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void setAplliedDiscount(DraftAppliedDiscount appliedDiscount) {
        if (allowEdit()) {
            this.appliedDiscount = appliedDiscount;
            this.calculatePrice();
            this.modifiedOn = Instant.now();
        }
    }

    public enum Status {
        open, completed
    }
}
