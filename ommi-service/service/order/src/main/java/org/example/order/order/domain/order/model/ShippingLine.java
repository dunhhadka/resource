package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "shipping_lines")
@NoArgsConstructor
public class ShippingLine {
    @JsonIgnore
    @ManyToOne
    @Setter
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Order order;

    @Id
    private int id;

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 50)
    private String source;

    @NotNull
    private BigDecimal price;

    @Size(max = 100)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "targetId", referencedColumnName = "id", updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Where(clause = "targetType = 'shipping")
    @OrderBy("id desc")
    private List<DiscountAllocation> discountAllocations;

    @Size(max = 100)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "targetId", referencedColumnName = "id", updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @Where(clause = "targetType = 'shipping")
    @OrderBy("id desc")
    private List<TaxLine> taxLines;

    public ShippingLine(
            int id,
            String title,
            String code,
            String source,
            BigDecimal price
    ) {
        this.id = id;
        this.title = title;
        this.code = code;
        this.source = source;
        this.price = price;
    }

    public void setTaxLines(List<TaxLine> taxLines) {
        if (CollectionUtils.isEmpty(taxLines)) return;
        this.taxLines = taxLines;
    }

    public void addAllocation(DiscountAllocation allocation) {
        if (CollectionUtils.isEmpty(this.discountAllocations)) this.discountAllocations = new ArrayList<>();
        this.discountAllocations.add(allocation);
    }

    public BigDecimal getTotalDiscount() {
        if (CollectionUtils.isEmpty(this.discountAllocations)) return BigDecimal.ZERO;
        return this.discountAllocations.stream()
                .map(DiscountAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalShippingTax() {
        if (CollectionUtils.isEmpty(this.taxLines)) return BigDecimal.ZERO;
        return this.taxLines.stream()
                .map(TaxLine::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
