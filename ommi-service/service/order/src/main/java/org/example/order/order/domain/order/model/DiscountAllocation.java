package org.example.order.order.domain.order.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.ddd.ValueObject;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "discount_allocations")
public class DiscountAllocation extends ValueObject<DiscountAllocation> {
    @Id
    private int id;

    @Min(1)
    private int storeId;

    @Min(1)
    private int orderId;

    @NotNull
    private BigDecimal amount;

    private Integer targetId;

    private TargetType targetType;

    private int applicationId;

    private int applicationIndex;

    private Instant createdAt;

    @Version
    private Integer version;

    public DiscountAllocation(
            int id,
            BigDecimal amount,
            int targetId,
            TargetType targetType,
            int applicationId,
            int applicationIndex
    ) {
        this.id = id;
        this.amount = amount;
        this.targetId = targetId;
        this.targetType = targetType;
        this.applicationId = applicationId;
        this.applicationIndex = applicationIndex;
    }

    public void setRootId(OrderId orderId) {
        this.storeId = orderId.getStoreId();
        this.id = orderId.getId();
    }

    public enum TargetType {
        line_item,
        shipping
    }
}
