package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.utils.NumberUtils;
import org.example.order.order.application.utils.OrderHelper;
import org.example.order.order.domain.order.persistence.OrderIdGenerator;
import org.example.order.order.domain.refund.model.Refund;
import org.example.order.order.domain.refund.model.RefundLineItem;
import org.example.order.order.domain.refund.model.RefundTaxLine;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.sound.sampled.Line;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Setter(AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "orders")
public non-sealed class Order extends AggregateRoot<Order> implements ApplyEditAction {

    public static final Currency DEFAULT_CURRENCY = Currency.getInstance("VND");

    @Transient
    @JsonIgnore
    private OrderIdGenerator orderIdGenerator;

    @JsonUnwrapped
    @EmbeddedId
    @AttributeOverride(name = "storeId", column = @Column(name = "storeId"))
    @AttributeOverride(name = "id", column = @Column(name = "id"))
    private OrderId id;

    @NotNull
    private Instant createdOn;

    @NotNull
    private Instant modifiedOn;

    private Instant processedOn;

    private Instant closedOn;

    private Instant cancelledOn;

    @Enumerated(value = EnumType.STRING)
    private CancelReason cancelReason;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private OrderStatus orderStatus;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private FinancialStatus financialStatus;

    @Enumerated(value = EnumType.STRING)
    private FulfillmentStatus fulfillmentStatus;

    @Enumerated(value = EnumType.STRING)
    private ReturnStatus returnStatus;

    @Min(0)
    private int totalWeight;

    private boolean test;

    @Size(max = 2000)
    private String note;

    @Valid
    @JsonUnwrapped
    @Embedded
    private ReferenceInfo referenceInfo;

    @Valid
    @JsonUnwrapped
    @Embedded
    private TracingInfo tracingInfo;

    @JsonUnwrapped
    @Valid
    @Embedded
    private CustomerInfo customerInfo;

    @JsonUnwrapped
    @Embedded
    @Valid
    private MoneyInfo moneyInfo;

    @JsonUnwrapped
    @Embedded
    @Valid
    private PaymentMethodInfo paymentMethodInfo;

    @Size(max = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_tags", joinColumns = {
            @JoinColumn(name = "storeId", referencedColumnName = "storeId"),
            @JoinColumn(name = "orderId", referencedColumnName = "id")
    })
    private List<@Valid OrderTag> tags = new ArrayList<>();

    @Size(max = 50)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_custom_attributes", joinColumns = {
            @JoinColumn(name = "storeId", referencedColumnName = "storeId"),
            @JoinColumn(name = "orderId", referencedColumnName = "id")
    })
    private List<@Valid OrderCustomAttribute> noteAttributes;

    @NotEmpty
    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid LineItem> lineItems;

    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid ShippingLine> shippingLines;

    @Size(max = 10)
    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid OrderDiscountCode> discountCodes;

    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid BillingAddress> billingAddresses;

    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid ShippingAddress> shippingAddresses;

    @Fetch(FetchMode.SUBSELECT)
    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<@Valid CombinationLine> combinationLines;

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid DiscountApplication> discountApplications;

    @OneToMany(mappedBy = "aggRoot", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.SUBSELECT)
    private Set<@Valid Refund> refunds;

    @OneToMany(mappedBy = "aggRoot", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private List<RefundTaxLine> refundTaxLines = new ArrayList<>();

    private boolean taxExempt;

    private boolean taxIncluded;

    private Integer locationId;
    private Integer userId;

    public Order(
            int storeId,
            Instant processedAt,
            CustomerInfo customerInfo,
            TracingInfo trackingInfo,
            Currency currency,
            String gateway,
            String processingMethod,
            int totalWeight,
            String note,
            List<String> tagInputs,
            Map<String, String> customAttributes,
            BillingAddress billingAddress,
            ShippingAddress shippingAddress,
            List<LineItem> lineItems,
            List<ShippingLine> shippingLines,
            List<OrderDiscountCode> discountCodes,
            List<DiscountApplication> discountApplications,
            List<DiscountAllocation> discountAllocations,
            OrderIdGenerator orderIdGenerator,
            boolean taxExempt,
            boolean taxesIncluded,
            Integer userId,
            Integer locationId,
            List<CombinationLine> combinationLines
    ) {
        this.generateOrderId(storeId, orderIdGenerator);

        this.internalSetCustomerInfo(customerInfo);
        this.internalSetTracingInfo(trackingInfo);

        this.note = note;
        this.mergeTags(tagInputs);
        this.mergeCustomAttributes(customAttributes);

        this.privateSetBillingAddress(billingAddress);
        this.privateSetShippingAddress(shippingAddress);

        this.moneyInfo = MoneyInfo.builder().currency(currency).build();

        this.privateSetCombinationLines(combinationLines);
        this.privateSetLineItems(lineItems);
        this.privateSetShippingLines(shippingLines);
        this.privateSetDiscountCodes(discountCodes);

        this.taxExempt = taxExempt;
        this.taxIncluded = taxesIncluded;

        this.allocateDiscounts(discountApplications, discountAllocations);
        this.totalWeight = this.calculateTotalWeight();
        this.orderStatus = OrderStatus.open;
        this.returnStatus = ReturnStatus.no_return;
        this.paymentMethodInfo = new PaymentMethodInfo(gateway, processingMethod);

        this.calculateMoneyForInsert();

        this.initFinanceStatus();

        this.locationId = locationId;
        this.userId = userId;

        this.processedOn = processedAt;
    }

    private void initFinanceStatus() {
        if (this.moneyInfo.getTotalPrice().compareTo(BigDecimal.ZERO) == 0) {
            this.changeFinanceStatus(FinancialStatus.paid);
        } else {
            this.changeFinanceStatus(FinancialStatus.pending);
        }
    }

    private void changeFinanceStatus(FinancialStatus status) {
        if (Objects.equals(this.financialStatus, status)) return;
        this.financialStatus = status;
        this.modifiedOn = Instant.now();
    }

    private void calculateMoneyForInsert() {
        BigDecimal lineItemDiscountedTotal = this.lineItems.stream()
                .map(LineItem::getDiscountedTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal productDiscountedTotal = this.lineItems.stream()
                .map(LineItem::getProductDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal orderLineItemDiscountedTotal = this.lineItems.stream()
                .map(LineItem::getOrderDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLineItemTax = this.lineItems.stream()
                .map(LineItem::getTotalTax)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalShippingPrice = BigDecimal.ZERO;
        BigDecimal shippingDiscount = BigDecimal.ZERO;
        BigDecimal totalShippingTax = BigDecimal.ZERO;

        if (CollectionUtils.isNotEmpty(shippingLines)) {
            totalShippingPrice = this.shippingLines.stream()
                    .map(ShippingLine::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            shippingDiscount = this.shippingLines.stream()
                    .map(ShippingLine::getTotalDiscount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalShippingTax = this.shippingLines.stream()
                    .map(ShippingLine::getTotalShippingTax)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal orderCartDiscount = orderLineItemDiscountedTotal.add(shippingDiscount);
        BigDecimal totalDiscounts = orderCartDiscount.add(productDiscountedTotal);

        BigDecimal totalTax = totalLineItemTax.add(totalShippingTax);

        BigDecimal totalLineItemPrice = lineItemDiscountedTotal.add(productDiscountedTotal);
        BigDecimal subtotalPrice = lineItemDiscountedTotal.subtract(orderLineItemDiscountedTotal);
        BigDecimal subtotalShippingPrice = totalShippingPrice.subtract(shippingDiscount);
        BigDecimal totalPrice = subtotalPrice.add(subtotalShippingPrice);

        if (!this.taxIncluded) {
            totalPrice = totalPrice.add(totalTax);
        }

        var moneyInfoBuilder = this.moneyInfo.toBuilder()
                .totalPrice(totalPrice)
                .subtotalPrice(subtotalPrice)
                .totalLineItemPrice(totalLineItemPrice)
                .totalShippingPrice(totalShippingPrice)
                .cartDiscountAmount(orderCartDiscount)
                .totalDiscounts(totalDiscounts)
                .totalTax(totalTax)
                .currentTotalPrice(totalPrice)
                .currentSubtotalPrice(subtotalPrice)
                .currentCartDiscountAmount(orderCartDiscount)
                .currentTotalDiscounts(totalDiscounts)
                .currentTotalTax(totalTax);

        moneyInfoBuilder
                .unpaidAmount(totalPrice)
                .totalOutstanding(totalPrice);

        moneyInfoBuilder
                .originalTotalPrice(totalPrice);

        this.moneyInfo = moneyInfoBuilder.build();
    }

    private int calculateTotalWeight() {
        long totalWeight = this.lineItems.stream().mapToLong(line -> line.getVariantInfo().getGrams()).sum();
        return totalWeight > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalWeight;
    }

    protected Order() {

    }

    private Map<Integer, LineItem> lineItemMap() {
        if (CollectionUtils.isEmpty(this.lineItems)) return Map.of();
        return this.lineItems.stream()
                .collect(Collectors.toMap(
                        LineItem::getId,
                        Function.identity()
                ));
    }

    private Map<Integer, ShippingLine> shippingLineMap() {
        if (CollectionUtils.isEmpty(this.shippingLines)) return Map.of();
        return this.shippingLines.stream()
                .collect(Collectors.toMap(
                        ShippingLine::getId,
                        Function.identity()
                ));
    }

    private void allocateDiscounts(List<DiscountApplication> discountApplications, List<DiscountAllocation> discountAllocations) {
        if (CollectionUtils.isEmpty(discountAllocations) || CollectionUtils.isEmpty(discountAllocations)) return;

        this.discountApplications = discountApplications;
        for (var application : discountApplications) {
            application.setAggRoot(this);
        }

        var shippingLineMap = this.shippingLineMap();
        var lineItemMap = this.lineItemMap();

        for (var allocation : discountAllocations) {
            allocation.setRootId(this.id);
            switch (allocation.getTargetType()) {
                case line_item -> {
                    var lineItem = lineItemMap.get(allocation.getTargetId());
                    assert lineItem != null;
                    lineItem.addAllocation(allocation);
                }
                case shipping -> {
                    var shippingLine = shippingLineMap.get(allocation.getTargetId());
                    assert shippingLine != null;
                    shippingLine.addAllocation(allocation);
                }
            }
        }
    }

    private void privateSetDiscountCodes(List<OrderDiscountCode> discountCodes) {
        if (CollectionUtils.isEmpty(discountCodes)) return;
        this.discountCodes = discountCodes;
        for (var discount : discountCodes) {
            discount.setOrder(this);
        }
    }

    private void privateSetShippingLines(List<ShippingLine> shippingLines) {
        if (CollectionUtils.isEmpty(shippingLines)) return;

        this.shippingLines = shippingLines;
        for (var shipping : shippingLines) {
            shipping.setOrder(this);
            if (CollectionUtils.isEmpty(shipping.getTaxLines()))
                continue;
            for (var tax : shipping.getTaxLines()) {
                tax.setRoot(this.id);
            }
        }
    }

    private void privateSetLineItems(List<LineItem> lineItems) {
        if (CollectionUtils.isEmpty(lineItems)) {
            throw new IllegalArgumentException("line_items must not be empty");
        }

        this.lineItems = lineItems;
        for (var line : lineItems) {
            line.setAggRoot(this);
            if (CollectionUtils.isEmpty(line.getTaxLines()))
                continue;
            for (var taxLine : line.getTaxLines()) {
                taxLine.setRoot(this.id);
            }
        }
    }

    private void privateSetCombinationLines(List<CombinationLine> combinationLines) {
        this.combinationLines = combinationLines;
        for (var combination : combinationLines) {
            combination.setAggRoot(this);
        }
    }

    private void privateSetShippingAddress(ShippingAddress shippingAddress) {
        if (shippingAddress == null) return;
        shippingAddress.setOrder(this);
    }

    private void privateSetBillingAddress(BillingAddress billingAddress) {
        if (billingAddress == null) return;
        billingAddress.setOrder(this);
    }

    private void mergeCustomAttributes(Map<String, String> customAttributes) {
        if (customAttributes == null) return;

        if (this.noteAttributes == null) {
            this.noteAttributes = new ArrayList<>();
        }

        for (var attribute : this.noteAttributes) {
            if (customAttributes.containsKey(attribute.getName())) continue;
            this.internalRemoveAttribute(attribute);
        }

        customAttributes.forEach(this::internalAddAttribute);
    }

    private void internalAddAttribute(String name, String value) {
        var found = this.noteAttributes.stream()
                .filter(a -> StringUtils.equals(a.getName(), name))
                .findFirst().orElse(null);
        if (found != null) {
            String oldValue = found.getValue();
            if (StringUtils.equals(oldValue, value)) return;
            found.updateValue(value);
        } else {
            var attribute = new OrderCustomAttribute(name, value);
            this.noteAttributes.add(attribute);
        }
        this.modifiedOn = Instant.now();
    }

    private void internalRemoveAttribute(OrderCustomAttribute attribute) {
        if (!this.noteAttributes.contains(attribute)) return;
        this.noteAttributes.remove(attribute);
        this.modifiedOn = Instant.now();
    }

    //NOTE: Chỉ update tags nếu newTags = array. Null => return
    private void mergeTags(List<String> newTags) {
        if (newTags == null) return;

        var currentTagValues = this.tags.stream().map(OrderTag::getName).toList();
        newTags = newTags.stream().sorted().toList();
        if (CollectionUtils.isEqualCollection(currentTagValues, newTags)) return;

        newTags.forEach(this::internalAddTag);

        for (var tag : this.tags) {
            if (newTags.contains(tag.getName())) continue;
            this.internalRemoveTag(tag);
        }
    }

    private void internalRemoveTag(OrderTag tag) {
        if (!this.tags.contains(tag)) return;
        this.tags.remove(tag);
        this.modifiedOn = Instant.now();
    }

    private void internalAddTag(String newTag) {
        if (this.tags.stream().anyMatch(tag -> StringUtils.equals(tag.getName(), newTag))) return;
        var tag = new OrderTag(newTag);
        this.tags.add(tag);
        this.modifiedOn = Instant.now();
    }

    private void internalSetTracingInfo(TracingInfo trackingInfo) {
        Objects.requireNonNull(trackingInfo);
        this.tracingInfo = trackingInfo;
    }

    private void internalSetCustomerInfo(CustomerInfo customerInfo) {
        Objects.requireNonNull(customerInfo, "required customer info");
        this.customerInfo = customerInfo;
    }

    private void generateOrderId(int storeId, OrderIdGenerator orderIdGenerator) {
        var id = orderIdGenerator.generateOrderId();
        this.id = new OrderId(storeId, id);
    }

    //Nếu đã fulfilled
    public void markAsFulfilled() {
        this.fulfillAllLineItems();
    }

    private void fulfillAllLineItems() {
        var fulfillableLineItems = this.lineItems.stream()
                .filter(line -> line.getFulfillableQuantity() > 0)
                .collect(Collectors.toMap(LineItem::getId, LineItem::getFulfillableQuantity));
        if (fulfillableLineItems.isEmpty()) {
            return;
        }
        this.updateFulfilledLineItems(fulfillableLineItems);
    }

    private void updateFulfilledLineItems(Map<Integer, Integer> fulfillLineItemMap) {
        fulfillLineItemMap.forEach((lineItemId, fulfillableQuantity) -> {
            var lineItem = this.lineItems.stream()
                    .filter(line -> line.getId() == lineItemId)
                    .findFirst()
                    .orElseThrow();
            lineItem.fulfill(fulfillableQuantity);
        });

        updateFulfillmentStatus();
        this.modifiedOn = Instant.now();
    }

    private void updateFulfillmentStatus() {
        int unfulfilled = 0;
        int fulfilled = 0;
        int restocked = 0;
        int totalLineItem = this.lineItems.size();
        for (var line : lineItems) {
            if (line.getFulfillmentStatus() == null) {
                unfulfilled++;
                continue;
            }
            switch (line.getFulfillmentStatus()) {
                case fulfilled -> fulfilled++;
                case restocked -> restocked++;
            }
        }
        var orderFulfillmentStatus = FulfillmentStatus.partial;
        if (unfulfilled == totalLineItem) {
            orderFulfillmentStatus = null;
        } else if (fulfilled == totalLineItem) {
            orderFulfillmentStatus = FulfillmentStatus.fulfilled;
        } else if (restocked == totalLineItem) {
            orderFulfillmentStatus = FulfillmentStatus.returned;
        }
        this.changeFulfillmentStatus(orderFulfillmentStatus);
    }

    private void changeFulfillmentStatus(FulfillmentStatus status) {
        if (Objects.equals(this.fulfillmentStatus, status)) return;
        this.fulfillmentStatus = status;
        this.modifiedOn = Instant.now();
    }

    public void updateAssignedLocation(Integer routingLocationId) {
        if (locationId == null || Objects.equals(this.locationId, routingLocationId)) {
            return;
        }

        this.locationId = routingLocationId;
        this.modifiedOn = Instant.now();
    }

    public ShippingAddress getShippingAddress() {
        if (CollectionUtils.isEmpty(this.shippingAddresses)) return null;
        return this.shippingAddresses.get(0);
    }

    public void recognizeTransaction(TransactionInput transactionInput) {
        if (Objects.requireNonNull(transactionInput.getKind()) == OrderTransaction.Kind.sale) {
            recognizeSaleTransaction(transactionInput);
        }
    }

    private void recognizeSaleTransaction(TransactionInput transactionInput) {
        if (OrderTransaction.Status.success.equals(transactionInput.getStatus())) {
            var amount = transactionInput.getAmount();
            var totalReceived = this.moneyInfo.getTotalReceived().add(amount);
            var netPay = this.moneyInfo.getNetPayment().add(amount);
            var unPaidAmount = this.moneyInfo.getUnpaidAmount().subtract(amount);
            var totalOutStanding = this.moneyInfo.getTotalOutstanding().subtract(amount);

            this.moneyInfo = this.moneyInfo.toBuilder()
                    .totalReceived(totalReceived)
                    .netPayment(netPay)
                    .unpaidAmount(unPaidAmount)
                    .totalOutstanding(totalOutStanding)
                    .build();
        }
    }

    public void recalculatePaymentState(List<OrderTransaction> transactions) {
        this.moneyInfo = OrderHelper.recalculateMoneyInfo(this, transactions);
    }

    public void addRefund(@NotNull Refund refund) {
        this.internalAddRefund(refund);
        this.updateRefundedLineItemsStatus(refund.getRefundLineItems());
        this.recognizeRefund(refund);
        var refundTransaction = TransactionInput.builder()
                .kind(OrderTransaction.Kind.refund)
                .amount(refund.getTotalRefunded())
                .build();
        this.updateFinancialStatus(refundTransaction);
    }

    private void updateFinancialStatus(TransactionInput refundTransaction) {
        var totalReceived = this.moneyInfo.getTotalReceived();
        if (totalReceived.compareTo(BigDecimal.ZERO) > 0) {
            var totalRefunded = this.moneyInfo.getTotalRefunded();
            if (totalRefunded.compareTo(BigDecimal.ZERO) > 0) {
                boolean isPartialRefund = totalReceived.compareTo(totalRefunded) > 0;
            }
        }
    }

    /**
     * Khi refund, cần tính lại các khoản tiền trong MoneyInfo
     */
    private void recognizeRefund(Refund refund) {
        var refundedProductSubtotal = refund.getLineItemSubtotalRefunded();

        var refundedProductTax = refund.getTotalCartDiscountRefund();

        var refundedCartLevelDiscount = refund.getTotalCartDiscountRefund();
    }

    private void updateRefundedLineItemsStatus(Set<RefundLineItem> refundLineItems) {
        var refundItemMap = new HashMap<Integer, List<RefundLineItem>>();
        for (var refundItem : refundLineItems) {
            var lineItemId = refundItem.getLineItemId();
            if (refundItemMap.containsKey(lineItemId)) {
                refundItemMap.get(lineItemId).add(refundItem);
            } else {
                var refundItemMapValue = new ArrayList<RefundLineItem>();
                refundItemMapValue.add(refundItem);
                refundItemMap.put(lineItemId, refundItemMapValue);
            }
        }
        for (var entry : refundItemMap.entrySet()) {
            var lineItem = this.lineItems.stream()
                    .filter(line -> line.getId() == entry.getKey())
                    .findFirst()
                    .orElseThrow();

            lineItem.refund(entry.getValue());
        }
    }

    private void internalAddRefund(Refund refund) {
        if (CollectionUtils.isEmpty(this.refunds)) this.refunds = new HashSet<>();
        refund.setAggRoot(this);
        this.refunds.add(refund);

        this.modifiedOn = Instant.now();
    }

    public void updateOrInsertRefundTaxLine(Map<Integer, BigDecimal> updatableRefundedTaxLines, List<RefundTaxLine> refundTaxLines) {
        this.internalUpdateRefundedTaxLines(updatableRefundedTaxLines);

        this.internalAddNewRefundTaxLines(refundTaxLines);
    }

    private void internalAddNewRefundTaxLines(List<RefundTaxLine> refundTaxLines) {
        if (CollectionUtils.isEmpty(refundTaxLines)) {
            return;
        }
        if (this.refundTaxLines == null) this.refundTaxLines = new ArrayList<>();

        refundTaxLines.forEach(refund -> refund.setAggRoot(this));

        this.refundTaxLines.addAll(refundTaxLines);
    }

    private void internalUpdateRefundedTaxLines(Map<Integer, BigDecimal> updatableRefundedTaxLines) {
        if (updatableRefundedTaxLines == null || updatableRefundedTaxLines.isEmpty()) return;

        var refundedTaxLineMap = this.getRefundedTaxLineMap();

        updatableRefundedTaxLines.forEach((taxLineId, totalAmount) -> {
            var refundedTaxLine = refundedTaxLineMap.get(taxLineId);
            if (refundedTaxLine == null) return;
            refundedTaxLine.updateAmount(totalAmount);
        });
    }

    private Map<Integer, RefundTaxLine> getRefundedTaxLineMap() {
        if (CollectionUtils.isEmpty(this.refundTaxLines)) {
            return Map.of();
        }
        return this.refundTaxLines.stream()
                .collect(Collectors.toMap(
                        RefundTaxLine::getTaxLineId,
                        Function.identity(),
                        (t1, t2) -> t2
                ));
    }

    public BillingAddress getBillingAddress() {
        if (CollectionUtils.isEmpty(this.billingAddresses)) return null;
        return this.billingAddresses.get(0);
    }

    @Override
    public Order order() {
        return this;
    }

    @Override
    public void orderModified() {
        this.modifiedOn = Instant.now();
    }

    public List<Pair<LineItem, BigDecimal>> increaseLineItems(Map<Integer, BigDecimal> increaseMap) {
        if (increaseMap.isEmpty()) return List.of();

        var lineItemMap = this.getLineItemMap();

        var results = new ArrayList<Pair<LineItem, BigDecimal>>();

        increaseMap.forEach((lineItemId, delta) -> {
            var lineItem = lineItemMap.get(lineItemId);

            var invalid = lineItem == null
                    || lineItem.getFulfillableQuantity() == 0
                    || CollectionUtils.isNotEmpty(lineItem.getDiscountAllocations())
                    || NumberUtils.isPositive(lineItem.getTotalDiscount());
            if (invalid) return;

            lineItem.increaseQuantity(delta);

            results.add(Pair.of(lineItem, delta));
        });

        if (!results.isEmpty()) {
            var increaseAmount = results.stream()
                    .map(pair -> pair.getValue().multiply(pair.getKey().getDiscountedUnitPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            this.adjustTotalPrice(increaseAmount, increaseAmount);

            this.orderModified();
        }

        return results;
    }

    private Map<Integer, LineItem> getLineItemMap() {
        return this.lineItems
                .stream()
                .collect(Collectors.toMap(LineItem::getId, Function.identity()));
    }

    public void applyNewTaxes(List<TaxLine> newTaxLines) {
        if (newTaxLines.isEmpty()) return;

        var lineItemTaxMap = newTaxLines.stream()
                .collect(Collectors.toMap(
                        TaxLine::getTargetId,
                        Function.identity(),
                        (first, second) -> second
                ));

        var lineItemMap = this.getLineItemMap();

        lineItemTaxMap.forEach((lineItemId, taxLine) -> {
            var lineItem = lineItemMap.get(lineItemId);
            if (lineItem == null) return;
            lineItem.applyTax(taxLine);
        });

        var totalTax = newTaxLines.stream()
                .map(TaxLine::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var moneyInfoBuilder = this.moneyInfo.toBuilder()
                .totalTax(this.moneyInfo.getTotalTax().add(totalTax))
                .currentTotalTax(this.moneyInfo.getTotalTax().add(totalTax));

        if (!this.isTaxIncluded()) {
            moneyInfoBuilder
                    .totalPrice(this.moneyInfo.getTotalPrice().add(totalTax))
                    .currentTotalTax(this.moneyInfo.getCurrentTotalTax().add(totalTax))
                    .unpaidAmount(this.moneyInfo.getUnpaidAmount().add(totalTax))
                    .totalOutstanding(this.moneyInfo.getTotalOutstanding().add(totalTax));
        }

        this.moneyInfo = moneyInfoBuilder.build();

        this.orderModified();
    }

    public enum ReturnStatus {
        in_process,
        no_return,
        returned
    }

    public enum FulfillmentStatus {
        fulfilled,
        partial,
        returned
    }

    public enum FinancialStatus {
        pending,
        authorized,
        partially_paid,
        paid,
        partially_refunded,
        refunded,
        voided
    }


    public enum OrderStatus {
        open,
        close,
        cancelled
    }

    public enum CancelReason {
        customer,
        inventory,
        wrong_item,
        duplicate,
        contact,
        other
    }

    @Getter
    @Builder
    public static class TransactionInput {
        private Integer id; // id của orderTransaction
        private Integer parentId;
        private String sourceName;
        private String gateway;
        private String authorization;
        private String errorCode;
        private BigDecimal amount;
        private OrderTransaction.Kind kind;
        private OrderTransaction.Status status;
    }
}
