package org.example.order.order.domain.edit.model;

import com.google.common.base.Verify;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.model.draftorder.TaxSettingValue;
import org.example.order.order.application.service.orderedit.OrderEditContext;
import org.example.order.order.application.service.orderedit.TaxContext;
import org.example.order.order.domain.order.model.DiscountAllocation;
import org.example.order.order.domain.order.model.LineItem;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Slf4j
@Getter
@Entity
@NoArgsConstructor
@Table(name = "order_edits")
public class OrderEdit extends AggregateRoot<OrderEdit> {

    @EmbeddedId
    private OrderEditId id;

    @Min(1)
    private int orderId;

    @Min(0)
    private int orderVersion;

    private boolean committed;

    private Instant committedAt;

    private Currency currency;

    private BigDecimal subtotalLineItemQuantity;

    private BigDecimal subtotalPrice;

    private BigDecimal cartDiscountAmount;

    private BigDecimal totalPrice;

    private BigDecimal totalOutstanding;

    @OneToMany(mappedBy = "orderEdit", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid AddedLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "orderEdit", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid AddedTaxLine> taxLines = new ArrayList<>();

    @OneToMany(mappedBy = "orderEdit", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid AddedDiscountApplication> discountApplications = new ArrayList<>();

    @OneToMany(mappedBy = "orderEdit", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<@Valid AddedDiscountAllocation> discountAllocations = new ArrayList<>();

    @OneToMany(mappedBy = "orderEdit", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<OrderStagedChange> changes = new ArrayList<>();

    @NotNull
    private Instant createdAt;

    @NotNull
    private Instant updatedAt;

    @Version
    private Integer version;

    public OrderEdit(
            OrderEditId id,
            int orderId,
            Currency currency,
            BigDecimal subtotalLineItemQuantity,
            BigDecimal subtotalPrice,
            BigDecimal cartDiscountAmount,
            BigDecimal totalPrice,
            BigDecimal totalOutstanding
    ) {
        this.id = id;
        this.orderId = orderId;

        this.currency = currency;

        this.subtotalLineItemQuantity = subtotalLineItemQuantity;
        this.subtotalPrice = subtotalPrice;
        this.cartDiscountAmount = cartDiscountAmount;
        this.totalPrice = totalPrice;
        this.totalOutstanding = totalOutstanding;

        this.orderVersion = 1;

        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addLineItem(AddedLineItem lineItem, TaxContext taxContext) {
        lineItem.setOrderEdit(this);
        this.lineItems.add(lineItem);

        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity.add(lineItem.getEditableQuantity());

        this.adjustPrice(lineItem.getEditableSubtotal());

        if (taxContext.isCalculateTax()) {
            var taxRate = taxContext.getAppyTaxRateFor(lineItem.getProductId());
            this.createNewTaxLine(lineItem, taxRate, taxContext.isTaxIncluded());
        }

        OrderStagedChange.ActionType type;
        OrderStagedChange.BaseAction action;
        if (lineItem.getVariantId() != null) {
            type = OrderStagedChange.ActionType.add_variant;
            action = OrderStagedChange.AddVariant.builder()
                    .variantId(lineItem.getVariantId())
                    .lineItemId(lineItem.getId())
                    .quantity(lineItem.getEditableQuantity())
                    .locationId(lineItem.getLocationId())
                    .build();
        } else {
            type = OrderStagedChange.ActionType.add_custom_item;
            action = OrderStagedChange.AddCustomItem.builder()
                    .title(lineItem.getTitle())
                    .quantity(lineItem.getEditableQuantity())
                    .locationId(lineItem.getLocationId())
                    .originalUnitPrice(lineItem.getOriginalUnitPrice())
                    .taxable(lineItem.isTaxable())
                    .requireShipping(lineItem.isRequireShipping())
                    .lineItemId(lineItem.getId())
                    .build();
        }

        var change = new OrderStagedChange(UUID.randomUUID(), type, action);
        change.setOrderEdit(this);
        this.changes.add(change);

        this.updatedAt = Instant.now();
    }

    private void createNewTaxLine(AddedLineItem lineItem, TaxSettingValue taxRate, boolean taxIncluded) {
        var taxLine = new AddedTaxLine(
                UUID.randomUUID(),
                taxRate.getTitle(),
                lineItem.getId().toString(),
                lineItem.getEditableQuantity(),
                taxRate.getRate(),
                taxIncluded,
                lineItem.getEditableSubtotal(),
                this.currency
        );

        taxLine.setOrderEdit(this);
        this.taxLines.add(taxLine);

        this.adjustTaxPrice(taxLine.getPrice(), taxIncluded);
    }

    private void adjustTaxPrice(BigDecimal price, boolean taxIncluded) {
        if (!taxIncluded) {
            this.totalPrice = this.totalPrice.add(price);
            this.totalOutstanding = this.totalOutstanding.add(price);
        }
    }

    private void adjustPrice(BigDecimal adjustmentAmount) {
        this.subtotalPrice = this.subtotalPrice.add(adjustmentAmount);
        this.totalPrice = this.totalPrice.add(adjustmentAmount);
        this.totalOutstanding = this.totalOutstanding.add(adjustmentAmount);
    }

    public boolean removeAddedLineItem(UUID lineItemId, TaxContext taxContext) {
        var lineItem = this.findLineItemById(lineItemId);

        this.lineItems.remove(lineItem);

        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity.subtract(lineItem.getEditableQuantity());
        this.adjustPrice(lineItem.getEditableSubtotal().negate());

        this.removeDiscount(lineItemId);

        List<AddedTaxLine> taxLinesOfLine = new ArrayList<>();
        if (lineItem.isTaxable()) {
            for (var taxLine : this.taxLines) {
                if (taxLine.getTargetId().equals(lineItemId.toString())) {
                    taxLinesOfLine.add(taxLine);
                    this.adjustTaxPrice(taxLine.getPrice(), taxContext.isTaxIncluded());
                }
            }
        }
        this.taxLines.removeAll(taxLinesOfLine);

        this.changes.removeIf(change -> {
            if (change.getType() == OrderStagedChange.ActionType.add_variant) {
                OrderStagedChange.AddVariant action = change.castedValue();
                return action.getLineItemId().equals(lineItemId);
            }
            if (change.getType() == OrderStagedChange.ActionType.add_custom_item) {
                OrderStagedChange.AddCustomItem action = change.castedValue();
                return action.getLineItemId().equals(lineItemId);
            }
            return false;
        });

        this.updatedAt = Instant.now();
        return true;
    }

    private void removeDiscount(UUID lineItemId) {
        var possiblyAllocation = this.discountAllocations.stream()
                .filter(discount -> discount.getLineItemId().equals(lineItemId))
                .findFirst();
        if (possiblyAllocation.isEmpty()) return;

        var allocation = possiblyAllocation.get();

        // NOTE: Hiện tại 1 discountApplication tham chiếu đến 1 discountAllocation => Remove Allocation sau đó remove Application
        this.discountApplications.removeIf(application -> application.getId().equals(allocation.getApplicationId()));

        this.discountAllocations.remove(allocation);

        this.changes.removeIf((change) -> {
            if (change.getType() == OrderStagedChange.ActionType.add_item_discount) {
                var action = (OrderStagedChange.AddItemDiscount) change.getValue();
                return action.getLineItemId().equals(lineItemId);
            }
            return false;
        });
    }

    private AddedLineItem findLineItemById(UUID lineItemId) {
        assert CollectionUtils.isNotEmpty(this.lineItems) : "Require LineItems is not empty";

        return this.lineItems.stream()
                .filter(line -> line.getId().equals(lineItemId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Line item not found by id = " + lineItemId));
    }

    public boolean adjustAddedLineItem(UUID lineItemId, TaxContext taxContext, BigDecimal requestedQuantity) {
        var lineItem = this.findLineItemById(lineItemId);

        BigDecimal currentLineItemQuantity = lineItem.getEditableQuantity();
        if (requestedQuantity.compareTo(currentLineItemQuantity) == 0) {
            if (log.isDebugEnabled()) {
                log.debug("Skipping adjust AddedLineItem quantity");
                return false;
            }
        }

        BigDecimal currentLineItemPrice = lineItem.getEditableSubtotal();

        lineItem.adjustQuantity(requestedQuantity);

        BigDecimal newLineItemPrice = lineItem.getEditableSubtotal();
        this.adjustPrice(newLineItemPrice.subtract(currentLineItemPrice));

        var allocatedDiscountAmount = lineItem.getTotalDiscount();
        if (allocatedDiscountAmount.signum() > 0) {
            for (var discount : discountAllocations) {
                if (discount.getLineItemId().equals(lineItemId)) {
                    discount.update(allocatedDiscountAmount);
                    break;
                }
            }
        }

        if (taxContext.isCalculateTax()) {
            for (var taxLine : taxLines) {
                if (!taxLine.getTargetId().equals(lineItemId.toString())) {
                    continue;
                }
                BigDecimal currentTaxPrice = taxLine.getPrice();
                taxLine.updatePrice(
                        lineItem.getEditableSubtotal(),
                        lineItem.getEditableQuantity(),
                        taxContext.isTaxIncluded(),
                        currency
                );
                this.adjustTaxPrice(taxLine.getPrice().subtract(currentTaxPrice), taxContext.isTaxIncluded());
                break;
            }
        }

        for (var change : this.changes) {
            if (change.getType() == OrderStagedChange.ActionType.add_variant) {
                var action = (OrderStagedChange.AddVariant) change.getValue();
                if (action.getLineItemId().equals(lineItemId)) {
                    var newAction = action.toBuilder()
                            .quantity(requestedQuantity)
                            .build();
                    change.updateEvent(newAction);
                    break;
                }
            }
            if (change.getType() == OrderStagedChange.ActionType.add_custom_item) {
                var action = (OrderStagedChange.AddCustomItem) change.getValue();
                if (action.getLineItemId().equals(lineItemId)) {
                    var newAction = action.toBuilder()
                            .quantity(requestedQuantity)
                            .build();
                    change.updateEvent(newAction);
                    break;
                }
            }
        }

        this.updatedAt = Instant.now();
        return true;
    }

    public boolean updateExistingLineQuantity(
            LineItem lineItem,
            BigDecimal requestedQuantity,
            boolean restock,
            TaxContext taxContext
    ) {
        final int lineItemId = lineItem.getId();

        BigDecimal currentQuantity = BigDecimal.valueOf(lineItem.getFulfillableQuantity());
        BigDecimal delta = requestedQuantity.subtract(currentQuantity);

        var stagedChangeOptional = this.changes.stream()
                .filter(change ->
                        change.getValue() instanceof OrderStagedChange.QuantityAdjustment adjustment
                                && adjustment.getLineItemId() == lineItemId)
                .findFirst();

        if (delta.signum() == 0) {
            // If LineItem
            //
            // no change
            if (stagedChangeOptional.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("No change for LineItem. Skipping adjust quantity with request = 0");
                }
                return false;
            }

            this.removeExistingChanges(lineItem, stagedChangeOptional.get(), taxContext);

            this.updatedAt = Instant.now();
            return true;
        }

        OrderStagedChange.ActionType type = delta.signum() > 0
                ? OrderStagedChange.ActionType.increment_item
                : OrderStagedChange.ActionType.decrement_item;
        OrderStagedChange.BaseAction action = delta.signum() > 0
                ? OrderStagedChange.IncrementItem.builder().lineItemId(lineItemId).delta(delta).build()
                : OrderStagedChange.DecrementItem.builder().lineItemId(lineItemId).delta(delta.negate()).restock(restock).build();

        Optional<AddedTaxLine> oldTaxLine = Optional.empty();
        if (stagedChangeOptional.isPresent()) {
            var change = stagedChangeOptional.get();
            BigDecimal oldDelta = getQuantityDeltaChange(change);

            change.updateEvent(type, action);
            this.updateTotal(oldDelta.subtract(delta), lineItem.getDiscountedUnitPrice());

            if (change.getValue() instanceof OrderStagedChange.IncrementItem) {
                oldTaxLine = this.taxLines.stream()
                        .filter(taxLine -> StringUtils.equals(taxLine.getTargetId(), String.valueOf(lineItemId)))
                        .findFirst();
            }
        } else {
            var change = new OrderStagedChange(UUID.randomUUID(), type, action);
            change.setOrderEdit(this);
            this.changes.add(change);

            this.updateTotal(delta, lineItem.getDiscountedUnitPrice());
        }

        if (oldTaxLine.isEmpty()) {
            this.createTaxLineForLineItemIfNotExist(action, taxContext, lineItem);
        } else if (action instanceof OrderStagedChange.DecrementItem) { // Nếu đã tồn tại taxLine, sau đó decrease thì sẽ xoá TaxLine đã thêm vào
            this.taxLines.remove(oldTaxLine.get());
        } else { // Nếu đã tồn tại TaxLine, sau đó increase thì sẽ thay đổi 1 lượng quantity, thì sẽ update lại taxLine cũ
            var taxLine = oldTaxLine.get();
            taxLine.updatePrice(
                    delta.multiply(lineItem.getDiscountedUnitPrice()),
                    delta,
                    taxContext.isTaxIncluded(),
                    currency
            );
        }

        this.updatedAt = Instant.now();
        return true;
    }

    private void createTaxLineForLineItemIfNotExist(OrderStagedChange.BaseAction action, TaxContext taxContext, LineItem lineItem) {
        if (action instanceof OrderStagedChange.IncrementItem incrementItem) {
            if (!taxContext.isCalculateTax()) {
                return;
            }

            var taxLine = this.createNewTaxLine(
                    lineItem,
                    taxContext.getAppyTaxRateFor(lineItem.getVariantInfo().getProductId()),
                    taxContext.isTaxIncluded(),
                    incrementItem.getDelta());

            this.adjustTaxPrice(taxLine.getPrice(), taxContext.isTaxIncluded());
        }
    }

    private AddedTaxLine createNewTaxLine(LineItem lineItem, @NotNull TaxSettingValue applyTaxRateFor, boolean taxIncluded, BigDecimal quantity) {
        var taxLine = new AddedTaxLine(
                UUID.randomUUID(),
                applyTaxRateFor.getTitle(),
                String.valueOf(lineItem.getId()),
                quantity,
                applyTaxRateFor.getRate(),
                taxIncluded,
                lineItem.getDiscountedUnitPrice().multiply(quantity),
                this.currency
        );
        taxLine.setOrderEdit(this);
        this.taxLines.add(taxLine);
        return taxLine;
    }

    private void removeExistingChanges(LineItem lineItem, OrderStagedChange quantityChange, TaxContext taxContext) {
        this.changes.remove(quantityChange);

        BigDecimal oldDelta = this.getQuantityDeltaChange(quantityChange);
        this.updateTotal(oldDelta, lineItem.getDiscountedUnitPrice());

        if (lineItem.isTaxable()) {
            String lineItemIdString = String.valueOf(lineItem.getId());
            AddedTaxLine lineItemTax = null;
            for (var taxLine : this.taxLines) {
                if (taxLine.getTargetId().equals(lineItemIdString)) {
                    lineItemTax = taxLine;
                    this.adjustTaxPrice(taxLine.getPrice().negate(), taxContext.isTaxIncluded());
                    break;
                }
            }
            if (lineItemTax != null) {
                this.taxLines.remove(lineItemTax);
            }
        }
    }

    private void updateTotal(BigDecimal delta, BigDecimal discountedUnitPrice) {
        this.subtotalLineItemQuantity = this.subtotalLineItemQuantity.add(delta);
        var adjustmentPrice = delta.multiply(discountedUnitPrice);
        this.adjustPrice(adjustmentPrice);
    }

    private BigDecimal getQuantityDeltaChange(OrderStagedChange quantityChange) {
        return switch (quantityChange.getType()) {
            case decrement_item -> ((OrderStagedChange.DecrementItem) quantityChange.getValue()).getDelta();
            case increment_item -> ((OrderStagedChange.IncrementItem) quantityChange.getValue()).getDelta().negate();
            default -> throw new IllegalStateException("Not supported for change type this here");
        };
    }

    public void removeLineItemDiscount(UUID lineItemId, TaxContext taxContext) {
        var lineItem = findLineItemById(lineItemId);

        BigDecimal adjustedAmount = lineItem.removeDiscount();
        this.adjustPrice(adjustedAmount);

        this.removeDiscount(lineItemId);

        this.reCalculateAddedTaxLine(
                String.valueOf(lineItem.getId()),
                lineItem.getEditableSubtotal(),
                lineItem.getEditableQuantity(),
                taxContext.isTaxIncluded()
        );
    }

    private void reCalculateAddedTaxLine(String lineItemId, BigDecimal subtotalPrice, BigDecimal quantity, boolean taxIncluded) {
        for (var taxLine : this.taxLines) {
            if (taxLine.getTargetId().equals(lineItemId)) {
                var differenceAmount = taxLine.updatePrice(
                        subtotalPrice,
                        quantity,
                        taxIncluded,
                        this.currency
                );

                this.adjustTaxPrice(differenceAmount, taxIncluded);

                break;
            }
        }
    }

    public void applyDiscount(
            UUID lineItemId,
            OrderEditContext.DiscountRequest request,
            TaxContext taxContext
    ) {
        var lineItem = this.findLineItemById(lineItemId);

        var allocatedDiscountAmount = this.allocateAmount(request, lineItem);
        lineItem.applyDiscount(allocatedDiscountAmount, currency);

        var possiblyAllocation = this.discountAllocations.stream()
                .filter(discount -> discount.getLineItemId().equals(lineItemId))
                .findFirst();

        var allocation = possiblyAllocation
                .map(possibly -> this.updateDiscount(possibly, request, lineItem, allocatedDiscountAmount))
                .orElseGet(() -> this.createNewDiscount(request, lineItem, allocatedDiscountAmount));

        this.reCalculateAddedTaxLine(lineItemId.toString(), lineItem.getEditableSubtotal(), lineItem.getEditableQuantity(), taxContext.isTaxIncluded());

        this.updatedAt = Instant.now();
    }

    private AddedDiscountAllocation createNewDiscount(
            OrderEditContext.DiscountRequest request,
            AddedLineItem lineItem,
            BigDecimal value
    ) {
        String description = request.description();
        var discountType = request.type();

        var application = new AddedDiscountApplication(
                UUID.randomUUID(),
                description,
                value,
                discountType
        );
        application.setOrderEdit(this);
        this.discountApplications.add(application);

        var allocation = new AddedDiscountAllocation(
                UUID.randomUUID(),
                application.getId(),
                lineItem.getId(),
                value
        );
        allocation.setOrderEdit(this);
        this.discountAllocations.add(allocation);

        OrderStagedChange.BaseAction action = OrderStagedChange.AddItemDiscount.builder()
                .lineItemId(lineItem.getId())
                .description(description)
                .value(value)
                .applicationId(application.getId())
                .allocationId(allocation.getId())
                .amount(value)
                .build();
        var stagedChange = new OrderStagedChange(
                UUID.randomUUID(),
                OrderStagedChange.ActionType.add_item_discount,
                action
        );
        stagedChange.setOrderEdit(this);
        this.changes.add(stagedChange);

        if (log.isDebugEnabled()) {
            log.debug("""
                                Inserting new discount:
                                - Application: {},
                                - Allocation: {},
                                - StagedChange: {}
                            """,
                    application,
                    allocation,
                    stagedChange
            );
        }

        this.adjustPrice(value.negate());

        return allocation;
    }

    private AddedDiscountAllocation updateDiscount(
            AddedDiscountAllocation allocation,
            OrderEditContext.DiscountRequest request,
            AddedLineItem lineItem,
            BigDecimal amount
    ) {
        var applicationId = allocation.getApplicationId();
        var application = this.discountApplications.stream()
                .filter(discount -> discount.getId().equals(applicationId))
                .findFirst()
                .orElseThrow();

        String description = request.description();
        var valueType = request.type();
        application.update(description, amount, valueType);

        // Update amount
        this.adjustPrice(amount.subtract(allocation.getAmount()));

        allocation.update(amount);

        List<OrderStagedChange> discountedChanges = this.changes.stream()
                .filter(change ->
                        change.getType() == OrderStagedChange.ActionType.add_item_discount
                                && (change.getValue() instanceof OrderStagedChange.AddItemDiscount item && item.getLineItemId().equals(lineItem.getId())))
                .toList();

        Verify.verify(discountedChanges.size() == 1,
                "Expect exactly 1 AddLineItemDiscount for a single Line Item. Actual: %d".formatted(changes.size()));

        OrderStagedChange change = discountedChanges.get(0);
        OrderStagedChange.AddItemDiscount action = (OrderStagedChange.AddItemDiscount) change.getValue();
        action = action.toBuilder()
                .description(description)
                .amount(amount)
                .value(amount)
                .build();
        change.updateEvent(action);

        if (log.isDebugEnabled()) {
            log.debug(
                    """
                            Updated discount to: 
                            - Application: {},
                            - Allocation: {},
                            - StagedChange: {}
                            """,
                    application,
                    allocation,
                    change
            );
        }

        return allocation;
    }

    private BigDecimal allocateAmount(OrderEditContext.DiscountRequest request, AddedLineItem lineItem) {
        return switch (request.type()) {
            case fixed_amount -> request.value();
            case percentage -> request.value().multiply(lineItem.getEditableSubtotal())
                    .movePointLeft(2)
                    .setScale(currency.getDefaultFractionDigits(), RoundingMode.UP);
        };
    }


}
