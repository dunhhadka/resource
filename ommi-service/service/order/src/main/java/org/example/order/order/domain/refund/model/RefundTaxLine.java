package org.example.order.order.domain.refund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.order.domain.order.model.Order;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(name = "refund_tax_lines")
@NoArgsConstructor
public class RefundTaxLine {

    @JsonIgnore
    @Setter
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Order aggRoot;

    @Id
    private int id;

    @Min(1)
    private int taxLineId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private Instant createdAt;

    public RefundTaxLine(
            int id,
            Integer taxLineId,
            BigDecimal amount
    ) {
        this.id = id;
        this.taxLineId = taxLineId;
        this.amount = amount;
        this.createdAt = Instant.now();
    }

    public void updateAmount(BigDecimal totalAmount) {
        this.amount = totalAmount;
    }
}
