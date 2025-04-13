package org.example.order.order.domain.fulfillment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.ddd.NestedDomainEntity;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "fulfillment_line_items")
public class FulfillmentLineItem extends NestedDomainEntity<Fulfillment> {
    @JsonIgnore
    @ManyToOne
    @Setter
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "fulfillmentId", referencedColumnName = "id")
    private Fulfillment fulfillment;

    @Id
    private int id;

    private Integer orderId;

    private Integer lineItemId;

    @Min(1)
    private int quantity;

    private Integer effectiveQuantity;

    @Version
    private Integer version;

    @CreationTimestamp
    private Instant createdOn;

    public FulfillmentLineItem(
            int id,
            int orderId,
            int lineItemId,
            Integer quantity,
            Integer effectiveQuantity
    ) {
        this.id = id;
        this.orderId = orderId;
        this.lineItemId = lineItemId;
        this.quantity = quantity;
        this.effectiveQuantity = effectiveQuantity;
    }

    @Override
    protected Fulfillment getAggRoot() {
        return this.fulfillment;
    }
}
