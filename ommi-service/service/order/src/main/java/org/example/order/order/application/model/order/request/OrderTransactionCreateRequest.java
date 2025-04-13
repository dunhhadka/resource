package org.example.order.order.application.model.order.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.PaymentMethodInfo;
import org.example.order.order.domain.transaction.model.OrderTransaction;
import org.example.order.order.domain.transaction.model.PaymentInfo;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
public class OrderTransactionCreateRequest {
    private Integer orderId;
    private Integer locationId;

    private @Size(max = 250) String gateway;
    private @Size(max = 20) String processingMethod;

    private @Size(max = 50) String authorization;
    private @Size(max = 50) String errorCode;
    private @Size(max = 500) String message;

    private @Size(max = 50) String sourceName;
    private @Size(max = 50) String deviceId;
    private @Min(0) Integer parentId;

    private BigDecimal amount;

    private @Size(max = 3) String currency;

    private boolean sendNotification;

    private Instant processedAt;

    private @NotNull OrderTransaction.Kind kind;
    private @NotNull OrderTransaction.Status status;

    private PaymentInfo paymentInfo;
}
