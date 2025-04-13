package org.example.order.order.domain.refund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.infrastructure.configuration.exception.ConstrainViolationException;
import org.example.order.order.domain.order.model.Order;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "refunds")
public class Refund {

    @Setter
    @JsonIgnore
    @ManyToOne
    @JoinColumns({
            @JoinColumn(name = "storeId", referencedColumnName = "storeId"),
            @JoinColumn(name = "orderId", referencedColumnName = "id")
    })
    private Order aggRoot;

    @Id
    private int id;

    @NotNull
    private Instant createdOn;

    @NotNull
    private Instant processedOn;

    private Integer userId;

    private Integer returnId;

    @Size(max = 1000)
    private String note;

    @OneToMany(mappedBy = "refund", cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.DETACH}, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private Set<RefundLineItem> refundLineItems;

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    private Set<OrderAdjustment> orderAdjustments;

    private BigDecimal totalRefunded;

    public Refund(
            int id,
            Set<RefundLineItem> refundLineItems,
            Set<OrderAdjustment> orderAdjustments,
            String note,
            Instant processedAt
    ) {
        this.id = id;
        this.createdOn = Instant.now();
        this.setProcessedAt(processedAt, createdOn);
        this.privateSetLineItems(refundLineItems);
        this.privateSerAdjustments(orderAdjustments);

        this.note = note;
    }

    private void privateSerAdjustments(Set<OrderAdjustment> orderAdjustments) {
        this.orderAdjustments = orderAdjustments == null ? new HashSet<>() : orderAdjustments;
        for (var adjustment : this.orderAdjustments) {
            adjustment.setRefund(this);
        }
    }

    private void privateSetLineItems(Set<RefundLineItem> refundLineItems) {
        this.refundLineItems = refundLineItems == null ? new HashSet<>() : refundLineItems;
        for (var refundLine : this.refundLineItems) {
            refundLine.setRefund(this);
        }
    }

    private void setProcessedAt(Instant processedAt, Instant defaultValue) {
        this.processedOn = processedAt != null ? processedAt : defaultValue;
        if (this.processedOn.isAfter(defaultValue)) {
            throw new ConstrainViolationException("processed_on", "");
        }
    }

    public BigDecimal getTotalCartDiscountRefund() {
        if (CollectionUtils.isEmpty(this.refundLineItems)) {
            return BigDecimal.ZERO;
        }
        return this.refundLineItems.stream()
                .map(RefundLineItem::getTotalCartDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getLineItemSubtotalRefunded() {
        if (CollectionUtils.isEmpty(this.refundLineItems)) {
            return BigDecimal.ZERO;
        }
        return this.refundLineItems.stream()
                .map(RefundLineItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalShippingRefund() {
        if (CollectionUtils.isEmpty(this.orderAdjustments)) {
            return BigDecimal.ZERO;
        }
        return this.orderAdjustments.stream()
                .filter(adjustment -> adjustment.getKind() == OrderAdjustment.RefundKind.shipping_refund)
                .map(OrderAdjustment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalDiscrepancy() {
        if (CollectionUtils.isEmpty(this.orderAdjustments)) {
            return BigDecimal.ZERO;
        }
        return this.orderAdjustments.stream()
                .filter(adjustment -> adjustment.getKind() == OrderAdjustment.RefundKind.refund_discrepancy)
                .map(OrderAdjustment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(this.refundLineItems)
                && CollectionUtils.isEmpty(this.orderAdjustments);
    }

    public void setTotalRefunded(BigDecimal refundedAmount) {
        this.totalRefunded = refundedAmount;
    }
}
