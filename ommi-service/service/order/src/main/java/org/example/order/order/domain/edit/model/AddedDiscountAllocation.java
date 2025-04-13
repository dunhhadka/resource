package org.example.order.order.domain.edit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "order_edit_discount_allocations")
public class AddedDiscountAllocation {

    @Setter
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "editingId", referencedColumnName = "id")
    private OrderEdit orderEdit;

    @Id
    private UUID id;

    @NotNull
    private BigDecimal amount;

    @Min(1)
    private UUID applicationId;

    private UUID lineItemId;

    @NotNull
    private Instant updatedAt;

    @Version
    private Integer version;

    public AddedDiscountAllocation(
            UUID id,
            UUID applicationId,
            UUID lineItemId,
            BigDecimal amount
    ) {
        this.id = id;
        this.applicationId = applicationId;
        this.lineItemId = lineItemId;
        this.amount = amount;
    }

    public void update(BigDecimal allocateDiscountAmount) {
        this.amount = allocateDiscountAmount;
        this.updatedAt = Instant.now();
    }
}
