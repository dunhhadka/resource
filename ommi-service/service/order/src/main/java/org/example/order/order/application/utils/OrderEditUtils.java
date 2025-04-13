package org.example.order.order.application.utils;

import com.google.common.base.Preconditions;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.example.order.order.application.service.orderedit.LineItemBuilder;
import org.example.order.order.domain.edit.model.OrderStagedChange;
import org.example.order.order.domain.order.model.BillingAddress;
import org.example.order.order.domain.order.model.MailingAddress;
import org.example.order.order.domain.order.model.Order;
import org.example.order.order.domain.order.model.ShippingAddress;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class OrderEditUtils {

    public static String getCountryCode(Order order) {
        return Optional.ofNullable(order.getBillingAddress())
                .map(BillingAddress::getAddress)
                .or(() -> Optional.ofNullable(order.getShippingAddress()).map(ShippingAddress::getAddress))
                .map(MailingAddress::getCountryCode)
                .orElse("VND");
    }

    public static Pair<UUID, Integer> parseLineItemId(String lineItemIdString) {
        if (StringUtils.isBlank(lineItemIdString)) {
            throw new ConstrainViolationException(
                    "line_item_id",
                    "required"
            );
        }

        try {
            if (lineItemIdString.contains("-")) {
                UUID lineItemId = UUID.fromString(lineItemIdString);
                return Pair.of(lineItemId, null);
            }
            Integer lineItemId = Integer.parseInt(lineItemIdString);
            return Pair.of(null, lineItemId);
        } catch (Exception exception) {
            // ignore
        }

        throw new ConstrainViolationException(
                "line_item_id",
                "can't resolve line item id"
        );
    }

    public static GroupedStagedChange groupOrderStagedChange(List<OrderStagedChange> stagedChanges) {
        if (CollectionUtils.isEmpty(stagedChanges)) {
            return GroupedStagedChange.DEFAULT;
        }

        List<OrderStagedChange.AddVariant> addVariants = new ArrayList<>();
        List<OrderStagedChange.AddCustomItem> addCustomItems = new ArrayList<>();
        List<OrderStagedChange.AddItemDiscount> addItemDiscounts = new ArrayList<>();
        List<OrderStagedChange.IncrementItem> incrementItems = new ArrayList<>();
        List<OrderStagedChange.DecrementItem> decrementItems = new ArrayList<>();

        stagedChanges
                .forEach(groupChange(
                        addVariants::add,
                        addCustomItems::add,
                        addItemDiscounts::add,
                        incrementItems::add,
                        decrementItems::add)
                );

        return new GroupedStagedChange(
                addVariants,
                addCustomItems,
                addItemDiscounts,
                incrementItems,
                decrementItems,
                stagedChanges
        );
    }

    private static Consumer<OrderStagedChange> groupChange(
            Consumer<OrderStagedChange.AddVariant> addVariantConsumer,
            Consumer<OrderStagedChange.AddCustomItem> addCustomItemConsumer,
            Consumer<OrderStagedChange.AddItemDiscount> addItemDiscountConsumer,
            Consumer<OrderStagedChange.IncrementItem> incrementItemConsumer,
            Consumer<OrderStagedChange.DecrementItem> decrementItemConsumer
    ) {
        return change -> {
            switch (change.getType()) {
                case add_variant -> addVariantConsumer.accept((OrderStagedChange.AddVariant) change.getValue());
                case add_custom_item ->
                        addCustomItemConsumer.accept((OrderStagedChange.AddCustomItem) change.getValue());
                case add_item_discount ->
                        addItemDiscountConsumer.accept((OrderStagedChange.AddItemDiscount) change.getValue());
                case increment_item ->
                        incrementItemConsumer.accept((OrderStagedChange.IncrementItem) change.getValue());
                case decrement_item ->
                        decrementItemConsumer.accept((OrderStagedChange.DecrementItem) change.getValue());
            }
        };
    }

    public static LineItemBuilder.QuantityAction resolveQuantityAction(OrderStagedChange.QuantityAdjustment quantityChange) {
        if (quantityChange == null)
            return LineItemBuilder.QuantityAction.defaultValue();

        if (quantityChange instanceof OrderStagedChange.IncrementItem increment)
            return new LineItemBuilder.QuantityAction(increment, null);
        else if (quantityChange instanceof OrderStagedChange.DecrementItem decrement)
            return new LineItemBuilder.QuantityAction(null, decrement);
        else throw new IllegalStateException("Unknown Implementation for QuantityAdjustment");
    }

    public record GroupedStagedChange(
            List<OrderStagedChange.AddVariant> addVariants,
            List<OrderStagedChange.AddCustomItem> addCustomItems,
            List<OrderStagedChange.AddItemDiscount> addItemDiscounts,
            List<OrderStagedChange.IncrementItem> incrementItems,
            List<OrderStagedChange.DecrementItem> decrementItems,
            List<OrderStagedChange> stagedChanges) {

        public static final GroupedStagedChange DEFAULT = new GroupedStagedChange(
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        public Stream<OrderStagedChange.QuantityAdjustment> quantityAdjustmentsStream() {
            return Stream.concat(this.incrementItems.stream(), this.decrementItems.stream());
        }

        public Stream<OrderStagedChange.AddLineItemAction> addLineItemActionsStream() {
            return Stream.concat(this.addVariants.stream(), this.addCustomItems.stream());
        }

        public OrderStagedChange find(OrderStagedChange.BaseAction action) {
            var changes = this.stagedChanges.stream()
                    .filter(c -> c.getValue() == action)
                    .toList();

            Preconditions.checkArgument(!changes.isEmpty(),
                    "Can't find an action from OrderEdit");

            return changes.get(0);
        }
    }
}
