package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.location.Location;
import org.example.order.ddd.NestedDomainEntity;
import org.example.order.order.domain.order.rule.FulfillLineItemRule;
import org.example.order.order.domain.refund.model.RefundLineItem;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "line_items")
public class LineItem extends NestedDomainEntity<Order> {
    @JsonIgnore
    @ManyToOne
    @Setter
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Order aggRoot;

    @Id
    private int id;

    @Min(1)
    private int quantity;

    @NotNull
    @Min(0)
    private BigDecimal price;

    private int fulfillableQuantity;

    @Enumerated(EnumType.STRING)
    private FulfillmentStatus fulfillmentStatus;

    @JsonUnwrapped
    @Valid
    @Embedded
    private VariantInfo variantInfo;

//    @Size(max = 100)
//    @Fetch(FetchMode.SUBSELECT)
//    @ElementCollection
//    @CollectionTable(name = "line_items_properties", joinColumns = {
//            @JoinColumn(name = "orderId", referencedColumnName = "orderId"),
//            @JoinColumn(name = "storeId", referencedColumnName = "storeId"),
//            @JoinColumn(name = "lineItemId", referencedColumnName = "id")
//    })
//    private List<OrderCustomAttribute> properties;

    @Size(max = 100)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @JoinColumn(name = "targetId", referencedColumnName = "id", updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Where(clause = "targetType = 'line_item")
    @OrderBy("id desc")
    private List<DiscountAllocation> discountAllocations;

    @Size(max = 100)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @JoinColumn(name = "targetId", referencedColumnName = "id", updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Where(clause = "targetType = 'line_item")
    @OrderBy("id desc")
    private List<TaxLine> taxLines;

    private boolean taxable;

    private BigDecimal discountedUnitPrice;

    private BigDecimal discountedTotal;

    private BigDecimal originalTotal;

    private int currentQuantity;

    //NOTE: Tổng số lượng sản phẩm của các refundLineItem có removal = tre
    private int nonFulfillableQuantity;

    private int refundableQuantity;

    private String combinationLineKey;

    @Transient
    private Integer editingLocationId;

    @Version
    private Integer version;

    public LineItem(
            int id,
            int quantity,
            BigDecimal price,
            VariantInfo variantInfo,
            List<OrderCustomAttribute> properties,
            List<TaxLine> taxLines,
            Boolean taxable,
            String discountCode,
            BigDecimal discount,
            String componentLineKey
    ) {
        this.id = id;
        this.quantity = quantity;
        this.price = price;
        this.fulfillableQuantity = quantity;

        this.internalSetVariantInfo(variantInfo);
        this.combinationLineKey = componentLineKey;

//        this.properties = properties;

        this.taxable = taxable;
        this.changeTax(taxLines);

        this.currentQuantity = this.quantity;
        this.refundableQuantity = this.quantity;

        this.originalTotal = this.price.multiply(BigDecimal.valueOf(this.quantity));
        this.discountedTotal = this.originalTotal;
        this.discountedUnitPrice = this.price;
    }


    public LineItem(
            int id,
            int quantity,
            BigDecimal price,
            VariantInfo variantInfo,
            boolean taxable
    ) {
        this.id = id;
        this.quantity = quantity;

        this.price = price;
        this.discountedUnitPrice = price;

        this.variantInfo = variantInfo;

        this.taxable = taxable;

        this.calculatePrice();
    }

    private void calculatePrice() {
        BigDecimal quantityDecimal = BigDecimal.valueOf(this.quantity);
        this.originalTotal = this.price.multiply(quantityDecimal);

        if (this.discountedUnitPrice.signum() == 0) {
            this.discountedTotal = this.originalTotal;
            return;
        }
        this.discountedTotal = this.discountedUnitPrice.multiply(quantityDecimal);
    }

    public void changeTax(List<TaxLine> taxLines) {
        if (CollectionUtils.isEmpty(taxLines)) return;
        this.taxLines = taxLines;
    }

    private void internalSetVariantInfo(VariantInfo variantInfo) {
        Objects.requireNonNull(variantInfo);
        // auto fill name for variantInfo
        StringBuilder nameBuilder = new StringBuilder(variantInfo.getTitle());
        String variantTitle = variantInfo.getVariantTitle();
        if (StringUtils.isNotBlank(variantTitle) && !"Default Title".equals(variantTitle)) {
            nameBuilder.append(" - ").append(variantTitle);
        }
        variantInfo.setName(nameBuilder.toString());
        this.variantInfo = variantInfo;
    }

    public void addAllocation(DiscountAllocation allocation) {
        if (this.discountAllocations == null) this.discountAllocations = new ArrayList<>();
        this.discountAllocations.add(allocation);
        this.calculateDiscount();
    }

    private void calculateDiscount() {
        assert this.aggRoot != null;

        if (this.price.signum() <= 0) return;
        var quantityDecimal = BigDecimal.valueOf(this.quantity);
        var totalPrice = this.price.multiply(quantityDecimal);

        var totalDiscount = getTotalDiscount().min(totalPrice);

        this.discountedTotal = totalPrice.subtract(totalDiscount);
        var currency = this.aggRoot.getMoneyInfo().getCurrency();
        this.discountedUnitPrice = this.discountedTotal
                .divide(quantityDecimal, currency.getDefaultFractionDigits(), RoundingMode.FLOOR);
    }

    public BigDecimal getTotalDiscount() {
        if (CollectionUtils.isEmpty(this.discountAllocations)) return BigDecimal.ZERO;
        return this.discountAllocations.stream()
                .map(DiscountAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getProductDiscount() {
        return filterDiscount(rule -> rule.getRuleType() == DiscountApplication.RuleType.product);
    }

    private BigDecimal filterDiscount(Predicate<DiscountApplication> condition) {
        assert this.aggRoot != null;

        if (CollectionUtils.isEmpty(this.discountAllocations)) return BigDecimal.ZERO;
        var applications = this.aggRoot.getDiscountApplications();
        return this.discountAllocations
                .stream()
                .filter(discount -> {
                    var application = applications.get(discount.getApplicationIndex());
                    return condition.test(application);
                })
                .map(DiscountAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getOrderDiscount() {
        return filterDiscount(rule -> rule.getRuleType() == DiscountApplication.RuleType.order);
    }

    public BigDecimal getTotalTax() {
        if (CollectionUtils.isEmpty(this.taxLines)) return BigDecimal.ZERO;
        return this.taxLines.stream()
                .map(TaxLine::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void fulfill(Integer fulfillQuantity) {
        this.checkRule(new FulfillLineItemRule(this.fulfillableQuantity, fulfillQuantity));
        this.fulfillableQuantity = this.fulfillableQuantity - fulfillQuantity;
        this.updateFulfillmentStatus();
    }

    private void updateFulfillmentStatus() {
        if (this.fulfillableQuantity == 0) {
            this.fulfillmentStatus = FulfillmentStatus.fulfilled;
        } else if (this.fulfillableQuantity > 0 && this.fulfillableQuantity < this.quantity - this.nonFulfillableQuantity) {
            this.fulfillmentStatus = FulfillmentStatus.partial;
        } else {
            this.fulfillmentStatus = null;
        }

    }

    public void refund(List<RefundLineItem> refundLineItems) {

        // add events
        var originalValues = List.of(
                this.currentQuantity,
                this.refundableQuantity,
                this.nonFulfillableQuantity,
                this.fulfillableQuantity
        );

        for (var refundItem : refundLineItems) {
            var refundQuantity = refundItem.getQuantity();
            this.currentQuantity -= refundQuantity;
            this.refundableQuantity -= refundQuantity;
            if (refundItem.isRemoval()) {
                this.fulfillableQuantity -= refundQuantity;
                this.nonFulfillableQuantity += refundQuantity;
            }
        }

        this.currentQuantity = Math.max(this.currentQuantity, 0);
        this.refundableQuantity = Math.max(this.refundableQuantity, 0);
        this.fulfillableQuantity = Math.max(this.fulfillableQuantity, 0);
        this.nonFulfillableQuantity = Math.min(this.quantity, this.nonFulfillableQuantity);
    }

    public LineItem withLocationId(Location location) {
        Objects.requireNonNull(location);
        this.editingLocationId = (int) location.getId();
        return this;
    }

    public void allocateDiscountAmount(BigDecimal discountAmount) {
        BigDecimal quantityDecimal = BigDecimal.valueOf(this.quantity);
        BigDecimal discountUnitPrice = discountAmount.signum() == 0
                ? BigDecimal.ZERO
                : discountAmount.divide(quantityDecimal, RoundingMode.HALF_UP);
        this.discountedUnitPrice = this.price.subtract(discountUnitPrice);

        this.calculatePrice();
    }

    public void increaseQuantity(BigDecimal delta) {
        int deltaInt = delta.intValue();

        this.quantity += deltaInt;
        this.currentQuantity += deltaInt;
        this.fulfillableQuantity += deltaInt;
        this.refundableQuantity += deltaInt;

        this.discountedTotal = this.originalTotal = this.price.multiply(BigDecimal.valueOf(this.quantity));
    }

    public void applyTax(TaxLine taxLine) {
        if (this.taxLines == null) this.taxLines = new ArrayList<>();
        this.taxLines.add(taxLine);
        taxLine.setRoot(this.aggRoot.getId());
    }

    public enum FulfillmentStatus {
        partial,
        fulfilled,
        restocked
    }
}
