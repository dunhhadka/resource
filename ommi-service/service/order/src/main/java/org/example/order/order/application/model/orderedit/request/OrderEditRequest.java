package org.example.order.order.application.model.orderedit.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class OrderEditRequest {

    @Getter
    @Setter
    public static class AddVariants {
        @NotEmpty
        @Size(max = 20)
        private List<@Valid AddVariant> addVariants;
    }

    @Getter
    @Setter
    public static class AddVariant {
        @Min(1)
        private int variantId;

        @Positive
        private BigDecimal quantity;

        @Min(0)
        private Integer locationId;

        private boolean allowDuplicate = true;
    }

    @Getter
    @Setter
    public static class AddCustomItem {
        @NotBlank
        @Size(max = 500)
        private String title;

        @NotNull
        @Min(0)
        private BigDecimal price;

        @Positive
        private BigDecimal quantity;

        @Min(0)
        private Integer locationId;

        private boolean requireShipping;

        private boolean taxable = true;
    }

    @Getter
    @Setter
    public static class SetItemQuantity {
        @NotBlank
        @Size(max = 36)
        private String lineItemId;

        @PositiveOrZero
        private BigDecimal quantity;

        private boolean restock;
    }

    @Getter
    @Setter
    public static class SetItemDiscount {
        private @NotNull UUID lineItemId;
        private @Size(max = 255) String description;
        private @Min(0) BigDecimal fixedValue;
        private @Min(0)
        @Max(100) BigDecimal percentValue;
    }

    @Getter
    @Setter
    public static class Commit {

    }
}
