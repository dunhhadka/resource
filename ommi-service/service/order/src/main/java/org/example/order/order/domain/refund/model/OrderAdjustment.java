package org.example.order.order.domain.refund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "order_adjustments")
public class OrderAdjustment {
    @JsonIgnore
    @ManyToOne
    @Setter
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "orderId")
    @JoinColumn(name = "refundId", referencedColumnName = "id")
    private Refund refund;

    @Id
    private int id;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private BigDecimal taxAmount;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private RefundKind kind;

    public OrderAdjustment(
            int id,
            BigDecimal amount,
            BigDecimal taxAmount,
            RefundKind refundKind
    ) {
        this.id = id;
        this.amount = amount;
        this.taxAmount = taxAmount;
        this.kind = refundKind;
    }

    public enum RefundKind {
        shipping_refund,
        refund_discrepancy
    }
}
