package org.example.order.order.application.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.domain.edit.model.OrderStagedChange;

import java.util.List;
import java.util.Optional;

public final class LineItemUtils {

    public static Optional<OrderStagedChange> quantityChangeOf(List<OrderStagedChange> changes, int lineItemId) {
        if (CollectionUtils.isEmpty(changes)) {
            return Optional.empty();
        }

        List<OrderStagedChange> changesOfLine = changes.stream()
                .filter(change -> isQuantityAdjustmentFor(change, lineItemId))
                .toList();
        if (changesOfLine.isEmpty()) {
            return Optional.empty();
        }

        if (changesOfLine.size() != 1) {
            throw new IllegalStateException(
                    "More than one quantity adjustment OrderStagedChange found for line item " + lineItemId
            );
        }

        return Optional.of(changesOfLine.get(0));
    }

    private static boolean isQuantityAdjustmentFor(OrderStagedChange change, int lineItemId) {
        var action = change.getValue();
        if (action instanceof OrderStagedChange.IncrementItem incrementItem) {
            return incrementItem.getLineItemId() == lineItemId;
        }
        if (action instanceof OrderStagedChange.DecrementItem decrementItem) {
            return decrementItem.getLineItemId() == lineItemId;
        }
        return false;
    }
}
