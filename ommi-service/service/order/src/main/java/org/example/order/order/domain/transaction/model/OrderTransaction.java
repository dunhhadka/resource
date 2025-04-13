package org.example.order.order.domain.transaction.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.example.order.ddd.AggregateRoot;
import org.example.order.order.application.converter.AbstractEnumConverter;
import org.example.order.order.application.converter.CustomEnumValue;
import org.example.order.order.application.converter.MapObjectConverter;
import org.example.order.order.domain.order.model.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Getter
@Entity
@Table(name = "order_transactions")
public class OrderTransaction extends AggregateRoot<OrderTransaction> {

    @EmbeddedId
    private OrderTransactionId id;

    private Integer orderId;

    private Integer refundId;

    /**
     *
     */
    private Integer parentId;

    @Size(max = 50)
    private String sourceName;

    @Valid
    @Embedded
    @JsonUnwrapped
    private ClientInfo clientInfo;

    @Size(max = 250)
    private String gateway;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    @Size(max = 3)
    private String currency;

    @NotNull
    @Convert(converter = Kind.Converter.class)
    private Kind kind;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private Status status;

    @Size(max = 50)
    private String authorization;

    @Size(max = 50)
    private String errorCode;

    @Size(max = 50)
    private String message;

    @Valid
    @Embedded
    @JsonUnwrapped
    private PaymentInfo paymentInfo;

    @Size(max = 50)
    @Convert(converter = MapObjectConverter.class)
    private Map<@NotBlank @Size(max = 50) String, Objects> receipt;

    private Instant processedAt;

    private Instant createdOn;

    @Version
    private Integer version;

    public void resolveOrder(Order order) {
        this.orderId = order.getId().getId();
        if (StringUtils.isBlank(this.sourceName)) {
            this.sourceName = order.getTracingInfo().getSourceName();
        }
    }

    public boolean isCaptureOrSale() {
        return this.kind == Kind.capture || this.kind == Kind.sale;
    }

    @RequiredArgsConstructor
    public enum Kind implements CustomEnumValue<String> {

        sale("sale"),
        authorization("authorization"),
        capture("capture"),
        refund("refund"),
        _void("void");

        private final String value;

        @Override
        public String getValue() {
            return this.value;
        }

        static class Converter extends AbstractEnumConverter<Kind, String> {
            public Converter() {
                super(Kind.class);
            }
        }
    }

    public enum Status {
        pending,
        success,
        failure,
        error
    }
}
