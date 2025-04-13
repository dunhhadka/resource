package org.example.order.order.domain.edit.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.order.domain.order.model.DiscountApplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "order_edit_discount_applications")
public class AddedDiscountApplication {

    @Setter
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "editingId", referencedColumnName = "id")
    private OrderEdit orderEdit;

    @Id
    private UUID id;

    @Size(max = 500)
    private String description;

    @NotNull
    @Min(0)
    private BigDecimal value;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private DiscountApplication.ValueType valueType;

    @NotNull
    @Enumerated(value = EnumType.STRING)
    private DiscountApplication.TargetType targetType;

    @NotNull
    private Instant updatedAt;

    @Version
    private Integer version;

    public AddedDiscountApplication(
            UUID id,
            String description,
            BigDecimal value,
            DiscountApplication.ValueType type
    ) {
        this.id = id;
        this.description = description;
        this.value = value;
        this.valueType = type;
        this.targetType = DiscountApplication.TargetType.line_item;
    }

    public void update(String description, BigDecimal amount, DiscountApplication.ValueType type) {
        this.description = description;
        this.value = amount;
        this.valueType = type;
        this.updatedAt = Instant.now();
    }
}
