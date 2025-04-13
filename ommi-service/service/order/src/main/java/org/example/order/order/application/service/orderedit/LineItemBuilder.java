package org.example.order.order.application.service.orderedit;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.model.orderedit.response.CalculatedDiscountAllocation;
import org.example.order.order.domain.edit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dao.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public final class LineItemBuilder extends AbstractLineItemBuilder<LineItemBuilder.Context> {

    public LineItemBuilder(LineItemDto lineItem, Context context) {
        super(lineItem, context);
    }

    public static BuilderSteps.Builder forLineItem(Context context) {
        return new LineItemBuilder(context.lineItem, context);
    }

    public record QuantityAction(
            OrderStagedChange.IncrementItem increment,
            OrderStagedChange.DecrementItem decrement
    ) {
        public static QuantityAction defaultValue() {
            return new QuantityAction(null, null);
        }

        public boolean changed() {
            return this.increment != null || this.decrement != null;
        }
    }

    public record ExistingTaxContext(
            TaxLineDto taxLine,
            List<RefundTaxLineDto> refundTaxLines
    ) {
    }

    public record Context(
            LineItemDto lineItem,
            QuantityAction action,
            List<ExistingTaxContext> existingTaxContexts,
            List<OrderEditTaxLineDto> newTaxLines,
            List<DiscountAllocationDto> allocations
    ) {
        public boolean hasDiscount() {
            return CollectionUtils.isNotEmpty(allocations);
        }
    }

    @Override
    protected void doBuild() {
        var action = context().action;
        if (action.changed()) {
            if (action.increment != null) {
                this.addChange(action.increment);
                this.applyChange(action.increment);
            } else {
                this.addChange(action.decrement);
                this.applyChange(action.decrement);
            }
        }

        applyChangePrice();
    }

    @Override
    protected Stream<? extends GenericTaxLine> streamTaxLines() {
        if (!context().lineItem.isTaxable()) {
            return Stream.empty();
        }

        var action = context().action;
        if (action == null) {
            return existingTaxLines();
        }

        if (action.increment != null) {
            return Stream.concat(existingTaxLines(), context().newTaxLines.stream());
        }

        OrderStagedChange.DecrementItem decrement = action.decrement;

        List<MergedTaxLine> originalTaxLines = context().existingTaxContexts.stream()
                .sorted(Comparator.<ExistingTaxContext>
                        comparingInt(taxContext -> taxContext.taxLine.getId()).reversed())
                .map(taxContext ->
                        new MergedTaxLine(MergedTaxLine.TaxLineKey.from(taxContext.taxLine))
                                .merge(taxContext.taxLine)
                                .mergeRefunds(taxContext.refundTaxLines))
                .toList();

        BigDecimal delta = decrement.getDelta();
        Iterator<MergedTaxLine> iterator = originalTaxLines.iterator();
        while (iterator.hasNext() && delta.signum() > 0) {
            MergedTaxLine mergedTaxLine = iterator.next();

            BigDecimal reduceQuantity = tryReduce(mergedTaxLine, delta);

            delta = delta.subtract(reduceQuantity);

            if (delta.signum() > 0) {
                iterator.remove();
            }
        }

        return originalTaxLines.stream();
    }

    private BigDecimal tryReduce(MergedTaxLine tax, BigDecimal remainQuantity) {
        var taxQuantity = tax.getQuantity();
        if (taxQuantity.compareTo(remainQuantity) <= 0) {
            tax.reduce(taxQuantity);
            return taxQuantity;
        }
        tax.reduce(remainQuantity);
        return remainQuantity;
    }

    private Stream<? extends GenericTaxLine> existingTaxLines() {
        return context().existingTaxContexts.stream()
                .map(taxContext ->
                        new MergedTaxLine(MergedTaxLine.TaxLineKey.from(taxContext.taxLine))
                                .merge(taxContext.taxLine)
                                .mergeRefunds(taxContext.refundTaxLines));
    }

    private void applyChangePrice() {
        if (context().hasDiscount()) {
            lineItem().setDiscountAllocations(
                    context().allocations.stream().map(CalculatedDiscountAllocation::new).toList());
        }

        BigDecimal quantity = lineItem().getQuantity();
        BigDecimal editableQuantity = lineItem().getEditableQuantity();

        BigDecimal totalPrice = lineItem().getOriginalUnitPrice().multiply(quantity);

        if (context().hasDiscount()) {
            Verify.verify(!context().action.changed());

            var totalDiscount = context().allocations.stream()
                    .map(DiscountAllocationDto::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalPrice = totalPrice.subtract(totalDiscount);
        }

        if (quantity.compareTo(BigDecimal.ZERO) > 0) {
            lineItem().setEditableSubtotal(
                    totalPrice
                            .divide(quantity, RoundingMode.HALF_UP)
                            .multiply(editableQuantity)
            );
        } else {
            lineItem().setEditableSubtotal(BigDecimal.ZERO);
        }

        lineItem().setUneditableSubtotal(totalPrice.subtract(lineItem().getEditableSubtotal()));
    }

    private void applyChange(OrderStagedChange.QuantityAdjustment action) {
        if (context().hasDiscount()) {
            Preconditions.checkArgument(action == null,
                    "Cannot adjust quantity of LineItem with discount");
            this.setQuantityForLineItem();
            return;
        }

        BigDecimal quantity = BigDecimal.valueOf(context().lineItem.getQuantity());
        BigDecimal editableQuantity = BigDecimal.valueOf(context().lineItem.getFulfillableQuantity());
        BigDecimal delta = action.getDelta();

        BigDecimal newQuantity;
        BigDecimal newEditableQuantity;
        if (action instanceof OrderStagedChange.IncrementItem) {
            newQuantity = quantity.add(delta);
            newEditableQuantity = editableQuantity.add(delta);
        } else if (action instanceof OrderStagedChange.DecrementItem decrement) {
            Verify.verify(quantity.compareTo(delta) >= 0,
                    "Error when decrease LineItem. Check again");
            newQuantity = quantity.subtract(delta);
            newEditableQuantity = editableQuantity.subtract(delta);
            lineItem().setRestockable(decrement.isRestock());
        } else throw new IllegalStateException();

        lineItem().setQuantity(newQuantity);
        lineItem().setEditableQuantity(newEditableQuantity);
        lineItem().setEditableQuantityBeforeChanges(newEditableQuantity);
    }

    private void setQuantityForLineItem() {
        BigDecimal quantity = BigDecimal.valueOf(context().lineItem.getQuantity());
        BigDecimal editableQuantity = BigDecimal.valueOf(context().lineItem.getFulfillableQuantity());

        lineItem().setQuantity(quantity);
        lineItem().setEditableQuantity(editableQuantity);
        lineItem().setEditableQuantityBeforeChanges(editableQuantity);
    }
}
