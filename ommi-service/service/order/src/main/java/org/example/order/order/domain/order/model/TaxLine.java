package org.example.order.order.domain.order.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tax_lines")
public class TaxLine {
    @Id
    private int id;

    @Positive
    private int storeId;

    @Positive
    private int orderId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @PositiveOrZero
    private BigDecimal price;

    @PositiveOrZero
    private BigDecimal rate;

    private Integer targetId;

    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    /**
     * Số lượng sản phẩm áp dụng taxLine
     */
    private Integer quantity;

    private boolean custom;

    @Version
    private Integer version;

    public TaxLine(
            Integer id,
            String title,
            BigDecimal price,
            BigDecimal rate,
            Integer targetId,
            TargetType targetType,
            int quantity
    ) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.rate = rate;
        this.targetId = targetId;
        this.targetType = targetType;
        this.quantity = quantity;
    }

    public void setRoot(OrderId orderId) {
        this.storeId = orderId.getStoreId();
        this.orderId = orderId.getId();
    }

    public enum TargetType {
        line_item,
        shipping
    }
}
