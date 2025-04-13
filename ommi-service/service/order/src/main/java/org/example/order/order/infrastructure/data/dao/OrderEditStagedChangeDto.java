package org.example.order.order.infrastructure.data.dao;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.edit.model.OrderStagedChange;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class OrderEditStagedChangeDto {
    private int storeId;
    private int editingId;

    private UUID id;

    private OrderStagedChange.ActionType type;

    private String value;

    private Instant updatedAt;

    private Integer version;
}
