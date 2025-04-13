package org.example.order.order.infrastructure.data.dao;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class RefundTaxLineDto {
    private int storeId;
    private int orderId;

    private int id;

    private int taxLineId;

    private BigDecimal amount;

    private Instant createdAt;
}
