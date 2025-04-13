package org.example.order.order.infrastructure.data.dao;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.order.model.TaxLine;

import java.math.BigDecimal;

@Getter
@Setter
public class TaxLineDto {
    private int id;

    private int storeId;

    private int orderId;

    private String title;

    private BigDecimal price;

    private BigDecimal rate;

    private Integer targetId;

    private TaxLine.TargetType targetType;

    /**
     * Số lượng sản phẩm áp dụng taxLine
     */
    private Integer quantity;

    private boolean custom;

    private Integer version;
}
