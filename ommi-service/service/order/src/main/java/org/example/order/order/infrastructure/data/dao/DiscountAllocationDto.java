package org.example.order.order.infrastructure.data.dao;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.DiscountAllocation;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class DiscountAllocationDto {
    private int id;

    private int storeId;

    private int orderId;

    private BigDecimal amount;

    private Integer targetId;

    private DiscountAllocation.TargetType targetType;

    private int applicationId;

    private int applicationIndex;

    private Instant createdAt;

    private Integer version;
}
