package org.example.order.order.domain.edit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.order.order.application.utils.JsonUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Slf4j
@Entity
@Table(name = "order_edit_staged_changes")
@NoArgsConstructor
public class OrderStagedChange {

    @Setter
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "editingId", referencedColumnName = "id")
    private OrderEdit orderEdit;

    @Id
    private UUID id;

    @Enumerated(value = EnumType.STRING)
    private ActionType type;

    @NotNull
    @Convert(converter = BaseAction.Converter.class)
    public OrderStagedChange.BaseAction value;

    private Instant updatedAt;

    @Version
    private Integer version;

    public OrderStagedChange(UUID id, ActionType type, BaseAction value) {
        this.id = id;
        this.type = type;
        this.value = value;
    }

    public void updateEvent(BaseAction newAction) {
        this.value = newAction;
        this.updatedAt = Instant.now();
    }

    public void updateEvent(ActionType type, BaseAction action) {
        this.type = type;
        this.value = action;
        this.updatedAt = Instant.now();
    }

    public interface QuantityAdjustment extends LineItemAction<Integer> {

        @Override
        Integer getLineItemId();

        BigDecimal getDelta();
    }

    public interface AddLineItemAction extends LineItemAction<UUID> {
        @Override
        UUID getLineItemId();

        BigDecimal getQuantity();

        Integer getLocationId();
    }

    public interface LineItemAction<T> {
        T getLineItemId();
    }

    public interface AddedLineItemAction {
        UUID lineItemId();

        Integer locationId();
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public <T extends BaseAction> T castedValue() {
        return (T) this.value;
    }

    @Getter
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = BaseAction.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = AddVariant.class, name = "add_variant"),
            @JsonSubTypes.Type(value = AddCustomItem.class, name = "add_custom_item"),
            @JsonSubTypes.Type(value = IncrementItem.class, name = "increment_item"),
            @JsonSubTypes.Type(value = DecrementItem.class, name = "decrement_item"),
            @JsonSubTypes.Type(value = AddItemDiscount.class, name = "add_item_discount")
    })
    public static class BaseAction {

        private final ActionType type;

        public BaseAction(ActionType type) {
            this.type = type;
        }

        @jakarta.persistence.Converter
        public static class Converter implements AttributeConverter<BaseAction, String> {

            @Override
            public String convertToDatabaseColumn(BaseAction baseAction) {
                try {
                    return JsonUtils.marshal(baseAction);
                } catch (JsonProcessingException e) {
                    log.error("can't serialize object to string value {}", baseAction);
                }
                return null;
            }

            @Override
            public BaseAction convertToEntityAttribute(String dbValue) {
                try {
                    return JsonUtils.unmarshal(dbValue, BaseAction.class);
                } catch (JsonProcessingException e) {
                    log.error("Can't deserialize value string to object {}", dbValue);

                }
                return null;
            }
        }
    }

    @Getter
    @Builder(toBuilder = true)
    public static class AddVariant extends BaseAction implements AddLineItemAction {
        @Positive
        private int variantId;
        private UUID lineItemId;
        @Positive
        private BigDecimal quantity;
        private Integer locationId;

        public AddVariant(int variantId, UUID lineItemId, BigDecimal quantity, Integer locationId) {
            this();
            this.variantId = variantId;
            this.lineItemId = lineItemId;
            this.quantity = quantity;
            this.locationId = locationId;
        }

        public AddVariant() {
            super(ActionType.add_variant);
        }
    }

    @Getter
    @Builder(toBuilder = true)
    public static class AddCustomItem extends BaseAction implements AddLineItemAction {

        @NotBlank
        @Size(max = 255)
        private String title;
        @Positive
        private BigDecimal quantity;
        @Min(1)
        private Integer locationId;

        private BigDecimal originalUnitPrice;

        private boolean taxable;
        private boolean requireShipping;

        private UUID lineItemId;

        public AddCustomItem(
                String title,
                BigDecimal quantity,
                Integer locationId,
                BigDecimal originalUnitPrice,
                boolean taxable,
                boolean requireShipping,
                UUID lineItemId
        ) {
            this();
            this.title = title;
            this.quantity = quantity;
            this.locationId = locationId;
            this.originalUnitPrice = originalUnitPrice;
            this.taxable = taxable;
            this.requireShipping = requireShipping;
            this.lineItemId = lineItemId;
        }

        public AddCustomItem() {
            super(ActionType.add_custom_item);
        }
    }

    @Getter
    @Builder
    public static class IncrementItem extends BaseAction implements QuantityAdjustment {

        @Positive
        private Integer lineItemId;

        @Positive
        private BigDecimal delta;

        public IncrementItem(Integer lineItemId, BigDecimal delta) {
            this();
            this.lineItemId = lineItemId;
            this.delta = delta;
        }

        public IncrementItem() {
            super(ActionType.increment_item);
        }
    }

    @Getter
    @Builder
    public static class DecrementItem extends BaseAction implements QuantityAdjustment {

        @Positive
        private Integer lineItemId;

        @Positive
        private BigDecimal delta;

        private boolean restock;

        public DecrementItem(Integer lineItemId, BigDecimal delta, boolean restock) {
            this();
            this.lineItemId = lineItemId;
            this.delta = delta;
            this.restock = restock;
        }

        public DecrementItem() {
            super(ActionType.decrement_item);
        }
    }

    @Getter
    @Builder(toBuilder = true)
    public static class AddItemDiscount extends BaseAction {

        private @NotNull UUID lineItemId;
        private @Size(max = 255) String description;
        private BigDecimal value;
        private UUID applicationId;
        private UUID allocationId;
        private BigDecimal amount;

        public AddItemDiscount() {
            super(ActionType.add_item_discount);
        }

        public AddItemDiscount(
                UUID lineItemId,
                String description,
                BigDecimal value,
                UUID applicationId,
                UUID allocationId,
                BigDecimal amount
        ) {
            this();
            this.lineItemId = lineItemId;
            this.description = description;
            this.value = value;
            this.applicationId = applicationId;
            this.allocationId = allocationId;
            this.amount = amount;
        }
    }

    public enum ActionType {
        add_variant,
        add_custom_item,
        increment_item,
        decrement_item,
        add_item_discount
    }
}
