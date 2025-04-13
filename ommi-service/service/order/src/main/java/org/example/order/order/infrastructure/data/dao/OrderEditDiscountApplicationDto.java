package org.example.order.order.infrastructure.data.dao;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.DiscountApplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class OrderEditDiscountApplicationDto {
    private int storeId;
    private int editingId;

    private UUID id;

    private String description;

    private BigDecimal value;

    private DiscountApplication.ValueType valueType;

    private DiscountApplication.TargetType targetType;

    private Instant updatedAt;

    private Integer version;
}
