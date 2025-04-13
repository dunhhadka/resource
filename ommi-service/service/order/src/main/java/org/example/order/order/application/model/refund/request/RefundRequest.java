package org.example.order.order.application.model.refund.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.example.order.order.application.model.order.request.OrderTransactionCreateRequest;
import org.example.order.order.domain.refund.model.RefundLineItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class RefundRequest {
    private Instant processedAt;

    private @Valid Cancel cancel;

    private @Valid Shipping shipping;

    private List<@Valid LineItem> refundLineItems;

    private @Size(max = 1000) String note;

    private List<@Valid OrderTransactionCreateRequest> transactions;

    private boolean sendNotification;

    private Option option;

    @Getter
    @Setter
    @NoArgsConstructor
    @Builder(toBuilder = true)
    @AllArgsConstructor
    public static class LineItem {
        private @Min(0) int lineItemId;
        private int quantity;
        private @Min(0) Integer locationId;

        private RefundLineItem.RestockType restockType;

        private boolean removal;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Shipping {
        private Boolean fullRefund;
        @PositiveOrZero
        private BigDecimal amount;
    }

    @Getter
    @Setter
    public static class Cancel {
        private boolean refundFullAmount = true;
        private boolean refundShipping = true;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class Option {
        public static final Option DEFAULT = new Option(true, false);
        private boolean create;
        private boolean editOrder;
    }

    public Option getOption() {
        return this.option == null ? Option.DEFAULT : this.option;
    }
}
