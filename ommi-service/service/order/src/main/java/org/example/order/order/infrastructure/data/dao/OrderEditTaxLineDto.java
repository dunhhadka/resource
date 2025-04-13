package org.example.order.order.infrastructure.data.dao;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.application.service.orderedit.GenericTaxLine;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class OrderEditTaxLineDto implements GenericTaxLine {
    private int storeId;
    private int editingId;

    private UUID id;

    private String targetId;

    private String title;

    private BigDecimal rate;

    private BigDecimal price;

    private BigDecimal quantity;

    private Instant updatedAt;

    private Integer version;
}
