package org.example.order.order.infrastructure.data.dao;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

@Getter
@Setter
public class OrderEditDto {
    private int storeId;
    private int id;
    private int orderId;

    private int orderVersion;

    private boolean committed;

    private Instant committedAt;

    private Currency currency;

    private BigDecimal subtotalLineItemQuantity;

    private BigDecimal subtotalPrice;

    private BigDecimal cartDiscountAmount;

    private BigDecimal totalPrice;

    private BigDecimal totalOutstanding;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer version;
}
