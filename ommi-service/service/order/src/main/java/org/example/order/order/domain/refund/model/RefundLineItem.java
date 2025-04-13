package org.example.order.order.domain.refund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.order.application.model.refund.response.RefundCalculationResponse;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "refund_line_items")
public class RefundLineItem {
    @JsonIgnore
    @ManyToOne
    @Setter
    @JoinColumn(name = "refundId", referencedColumnName = "id")
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "orderId")
    private Refund refund;

    @Id
    private int id;

    private int lineItemId;

    private int quantity;

    private Integer locationId;

    private BigDecimal price;

    private BigDecimal subtotal;

    private BigDecimal totalTax;

    private BigDecimal totalCartDiscount;

    private boolean removal;

    @Enumerated(value = EnumType.STRING)
    private RestockType type;

    public RefundLineItem(Integer id, RefundCalculationResponse.LineItem suggestedLineItem) {

    }

    public enum RestockType {
        no_restock,
        cancel, // Hoàn kho khi chưa fulfill
        _refund, // Hoàn kho khi đã fulfill
    }
}
