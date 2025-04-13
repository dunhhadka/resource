package org.example.order.order.domain.payment.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.example.order.order.application.converter.MapObjectConverter;
import org.example.order.order.domain.order.model.PaymentMethodInfo;
import org.example.order.order.domain.transaction.model.ClientInfo;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Getter
@Entity
@Table(name = "payments")
public class Payment {

    @EmbeddedId
    private PaymentId id;

    @NotBlank
    private String uniqueToken;

    @JsonUnwrapped
    @Embedded
    @Valid
    private PaymentIdentifyMutation identifyMutation;

    @JsonUnwrapped
    @Embedded
    private PaymentMethodInfo paymentMethod;

    @Size(max = 50)
    private String sourceName;

    private Integer locationId;

    @Valid
    @JsonUnwrapped
    @Embedded
    private ClientInfo clientInfo = new ClientInfo();

    @Enumerated(value = EnumType.STRING)
    private PaymentStatus status;

    @Size(max = 500)
    private String message;

    @AttributeOverrides({
            @AttributeOverride(name = "redirectUrl", column = @Column(name = "nextActionRedirectUrl")),
            @AttributeOverride(name = "qrCode", column = @Column(name = "nextActionQrCode")),
            @AttributeOverride(name = "terminalId", column = @Column(name = "nextActionTerminalId"))
    })
    @Embedded
    private @Valid PaymentNextAction nextAction;

    @AttributeOverrides({
            @AttributeOverride(name = "accountNumber", column = @Column(name = "receiverAccountNumber")),
            @AttributeOverride(name = "accountName", column = @Column(name = "receiverAccountName")),
            @AttributeOverride(name = "description", column = @Column(name = "receiverDescription"))
    })
    @Embedded
    @Valid
    private ReceiverInfo receiverInfo;

    @AttributeOverrides({
            @AttributeOverride(name = "status", column = @Column(name = "checkoutCompletionStatus")),
            @AttributeOverride(name = "message", column = @Column(name = "checkoutCompletionMessage"))
    })
    @Embedded
    @Valid
    private CheckoutCompletion checkoutCompletion;

    private String currency;

    private BigDecimal amount;

    private String reference;

    @Size(max = 255)
    private String billNumber;

    @Size(max = 255)
    private String note;

    @Size(max = 50)
    @Convert(converter = MapObjectConverter.class)
    private Map<@NotBlank @Size(max = 50) String, Object> receipt;

    @Version
    private int version;

    private Instant completedOn;

    private Instant cancelledOn;

    @CreationTimestamp
    private Instant createdOn;

    @UpdateTimestamp
    private Instant modifiedOn;

    public enum PaymentStatus {
        pending, processing, success, failure
    }
}
