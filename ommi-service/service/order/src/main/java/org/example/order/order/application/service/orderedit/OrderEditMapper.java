package org.example.order.order.application.service.orderedit;

import org.apache.commons.collections4.iterators.EmptyOrderedIterator;
import org.example.order.order.application.model.orderedit.response.OrderStagedChangeModel;
import org.example.order.order.domain.edit.model.OrderStagedChange;

public final class OrderEditMapper {

    public static OrderStagedChangeModel map(OrderStagedChange.AddVariant addVariant) {
        var model = new OrderStagedChangeModel.AddedVariant();
        model.setLineItemId(addVariant.getLineItemId());
        model.setQuantity(addVariant.getQuantity());
        model.setVariantId(addVariant.getVariantId());
        model.setLocationId(addVariant.getLocationId());
        return model;
    }

    public static OrderStagedChangeModel map(OrderStagedChange.AddCustomItem addCustomItem) {
        var model = new OrderStagedChangeModel.AddCustomItem();
        model.setTitle(addCustomItem.getTitle());
        model.setQuantity(addCustomItem.getQuantity());
        model.setOriginalUnitPrice(addCustomItem.getOriginalUnitPrice());
        model.setRequireShipping(addCustomItem.isRequireShipping());
        model.setLineItemId(addCustomItem.getLineItemId());
        model.setLocationId(addCustomItem.getLocationId());
        return model;
    }

    public static OrderStagedChangeModel map(OrderStagedChange.AddItemDiscount addItemDiscount) {
        var model = new OrderStagedChangeModel.AddedItemDiscount();
        model.setValue(addItemDiscount.getValue());
        model.setDescription(addItemDiscount.getDescription());
        model.setLineItemId(addItemDiscount.getLineItemId());
        model.setApplicationId(addItemDiscount.getApplicationId());
        model.setAllocationId(addItemDiscount.getAllocationId());
        return model;
    }

    public static OrderStagedChangeModel map(OrderStagedChange.IncrementItem incrementItem) {
        var model = new OrderStagedChangeModel.IncrementItem();
        model.setLineItemId(incrementItem.getLineItemId());
        model.setDelta(incrementItem.getDelta());
        return model;
    }

    public static OrderStagedChangeModel map(OrderStagedChange.DecrementItem decrementItem) {
        var model = new OrderStagedChangeModel.DecrementItem();
        model.setLineItemId(decrementItem.getLineItemId());
        model.setDelta(decrementItem.getDelta());
        model.setRestock(decrementItem.isRestock());
        return model;
    }

    public static OrderStagedChangeModel map(OrderStagedChange change) {
        return switch (change.getType()) {
            case add_variant -> map((OrderStagedChange.AddVariant) change.getValue());
            case add_custom_item -> map((OrderStagedChange.AddCustomItem) change.getValue());
            case increment_item -> map((OrderStagedChange.IncrementItem) change.getValue());
            case decrement_item -> map((OrderStagedChange.DecrementItem) change.getValue());
            case add_item_discount -> map((OrderStagedChange.AddItemDiscount) change.getValue());
        };
    }
}
