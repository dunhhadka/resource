package org.example.order.order.infrastructure.data.dao;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class OrderEditDiscountAllocationDto {
    private int storeId;
    private int editingId;

    private UUID id;

    private BigDecimal amount;

    private UUID applicationId;

    private UUID lineItemId;

    private Instant updatedAt;

    private Integer version;
}
