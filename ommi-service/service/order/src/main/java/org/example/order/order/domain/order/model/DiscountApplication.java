package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "discount_applications")
public class DiscountApplication {
    @ManyToOne
    @JsonIgnore
    @Setter
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Order aggRoot;

    @Id
    private int id;

    @NotNull
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    private ValueType valueType;

    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @NotNull
    private Instant createdAt;

    @Version
    private Integer version;

    @Size(max = 255)
    private String code;

    @Size(max = 250)
    private String title;

    @Size(max = 250)
    private String description;

    @Enumerated(value = EnumType.STRING)
    private RuleType ruleType;

    public DiscountApplication(
            int id,
            BigDecimal value,
            ValueType valueType,
            TargetType targetType,
            RuleType ruleType,
            String code,
            String title,
            String description
    ) {
        this.id = id;
        this.value = value;
        this.valueType = valueType;
        this.targetType = targetType;
        this.code = code;
        this.title = title;
        this.description = description;
        this.ruleType = ruleType;

        this.createdAt = Instant.now();
    }

    public enum TargetType {
        line_item,
        shipping_line
    }

    public enum ValueType {
        fixed_amount,
        percentage
    }

    public enum RuleType {
        order, product
    }
}
