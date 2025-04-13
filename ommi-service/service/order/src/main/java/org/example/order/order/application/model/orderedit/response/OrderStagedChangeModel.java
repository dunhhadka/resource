package org.example.order.order.application.model.orderedit.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.order.order.domain.edit.model.OrderStagedChange;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
public class OrderStagedChangeModel {
    protected final OrderStagedChange.ActionType type;


    @Getter
    @Setter
    public static class AddedVariant extends OrderStagedChangeModel {

        private BigDecimal quantity;
        private int variantId;
        private UUID lineItemId;
        private Integer locationId;

        public AddedVariant() {
            super(OrderStagedChange.ActionType.add_variant);
        }
    }

    @Getter
    @Setter
    public static class AddCustomItem extends OrderStagedChangeModel {
        private String title;
        private BigDecimal quantity;
        private BigDecimal originalUnitPrice;
        private boolean requireShipping;
        private UUID lineItemId;
        private Integer locationId;

        public AddCustomItem() {
            super(OrderStagedChange.ActionType.add_custom_item);
        }
    }

    @Getter
    @Setter
    public static class AddedItemDiscount extends OrderStagedChangeModel {

        private BigDecimal value;
        private String description;
        private UUID lineItemId;
        private UUID applicationId;
        private UUID allocationId;

        public AddedItemDiscount() {
            super(OrderStagedChange.ActionType.add_item_discount);
        }
    }

    @Getter
    @Setter
    public static class IncrementItem extends OrderStagedChangeModel {

        private int lineItemId;
        private BigDecimal delta;

        public IncrementItem() {
            super(OrderStagedChange.ActionType.increment_item);
        }
    }

    @Getter
    @Setter
    public static class DecrementItem extends OrderStagedChangeModel {

        private int lineItemId;
        private BigDecimal delta;
        private boolean restock;

        public DecrementItem() {
            super(OrderStagedChange.ActionType.decrement_item);
        }
    }
}
