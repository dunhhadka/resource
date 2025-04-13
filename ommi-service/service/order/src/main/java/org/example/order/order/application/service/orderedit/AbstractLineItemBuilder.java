package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.model.orderedit.response.CalculatedLineItem;
import org.example.order.order.application.model.orderedit.response.OrderStagedChangeModel;
import org.example.order.order.domain.edit.model.OrderStagedChange;
import org.example.order.order.infrastructure.data.dao.LineItemDto;
import org.example.order.order.infrastructure.data.dao.OrderEditLineItemDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class AbstractLineItemBuilder<T>
        implements BuilderSteps.Builder, BuilderSteps.Result {

    private final CalculatedLineItem lineItem;
    private final T context;
    private final List<OrderStagedChangeModel> changes;

    protected AbstractLineItemBuilder(CalculatedLineItem lineItem, T context) {
        this.lineItem = lineItem;
        this.context = context;
        this.changes = new ArrayList<>();
    }

    protected AbstractLineItemBuilder(LineItemDto lineItem, T context) {
        this(new CalculatedLineItem(lineItem), context);
    }

    protected AbstractLineItemBuilder(OrderEditLineItemDto lineItem, T context) {
        this(new CalculatedLineItem(lineItem), context);
    }

    @Override
    public BuilderSteps.Result build() {
        doBuild();

        this.lineItem.setStagedChanges(this.changes);

        return this;
    }

    protected final void addChange(OrderStagedChange.AddLineItemAction addAction) {
        if (addAction instanceof OrderStagedChange.AddVariant av) this.changes.add(OrderEditMapper.map(av));
        else if (addAction instanceof OrderStagedChange.AddCustomItem aci) this.changes.add(OrderEditMapper.map(aci));
        else throw new IllegalArgumentException("Unknown implementation of AddLineItemAction");
    }

    protected final void addChange(OrderStagedChange.AddItemDiscount addItemDiscount) {
        this.changes.add(OrderEditMapper.map(addItemDiscount));
    }

    protected abstract void doBuild();

    @Override
    public CalculatedLineItem lineItem() {
        return this.lineItem;
    }

    public T context() {
        return this.context;
    }

    protected void addChange(OrderStagedChange.QuantityAdjustment action) {
        if (action instanceof OrderStagedChange.IncrementItem ii) this.changes.add(OrderEditMapper.map(ii));
        else if (action instanceof OrderStagedChange.DecrementItem di) this.changes.add(OrderEditMapper.map(di));
        else throw new IllegalStateException("Unknown implementation");
    }

    @Override
    public Map<MergedTaxLine.TaxLineKey, MergedTaxLine> taxLines() {
        return Map.of();
    }

    protected abstract Stream<? extends GenericTaxLine> streamTaxLines();
}
