package org.example.order.order.application.service.orderedit;

import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.application.model.orderedit.response.CalculatedDiscountAllocation;
import org.example.order.order.domain.edit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dao.OrderEditDiscountAllocationDto;
import org.example.order.order.infrastructure.data.dao.OrderEditLineItemDto;
import org.example.order.order.infrastructure.data.dao.OrderEditTaxLineDto;

import java.util.List;
import java.util.stream.Stream;

public final class AddedLineItemBuilder extends AbstractLineItemBuilder<AddedLineItemBuilder.Context> {

    private AddedLineItemBuilder(OrderEditLineItemDto lineItem, Context context) {
        super(lineItem, context);
    }

    @Override
    protected void doBuild() {
        this.addChange(context().changes().addAction);

        if (context().changes().addDiscount != null) {
            this.addChange(context().changes().addDiscount);

            this.lineItem().setDiscountAllocations(
                    context().allocations.stream()
                            .map(CalculatedDiscountAllocation::new)
                            .toList()
            );
        }
    }

    @Override
    protected Stream<? extends GenericTaxLine> streamTaxLines() {
        return CollectionUtils.isEmpty(context().taxLines)
                ? Stream.empty()
                : context().taxLines.stream();
    }

    public static BuilderSteps.Builder forLineItem(OrderEditLineItemDto lineItem, Context context) {
        return new AddedLineItemBuilder(lineItem, context);
    }

    record Changes(
            OrderStagedChange.AddLineItemAction addAction,
            OrderStagedChange.AddItemDiscount addDiscount
    ) {
    }

    public record Context(
            Changes changes,
            List<OrderEditTaxLineDto> taxLines,
            List<OrderEditDiscountAllocationDto> allocations
    ) {

    }
}
