package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.order.application.utils.BigDecimals;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "order_discounts")
public class OrderDiscountCode {
    @JsonIgnore
    @Setter(AccessLevel.PACKAGE)
    @ManyToOne
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    private Order order;

    @Id
    private int id;

    @Size(max = 255)
    private String code;

    @Setter
    @NotNull
    @DecimalMin("0")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private ValueType type;

    @JsonIgnore
    @Transient
    private BigDecimal value;

    private Boolean custom;

    public OrderDiscountCode(
            int id,
            String code,
            BigDecimal value,
            ValueType valueType,
            boolean custom
    ) {
        this.id = id;
        this.code = code;
        this.value = value;
        this.type = valueType;
        this.custom = custom;

        // pre-calculate
        this.preCalculate();
    }

    private void preCalculate() {
        if (this.type == ValueType.percentage) {
            this.value = this.value.min(BigDecimals.ONE_HUND0RED);
        }
        this.amount = this.value;
    }

    //region enum

    public enum ValueType {
        fixed_amount,
        percentage,
        @Deprecated
        shipping_line,
        shipping
    }
}
