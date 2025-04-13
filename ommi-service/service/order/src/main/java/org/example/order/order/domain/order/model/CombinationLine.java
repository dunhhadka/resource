package org.example.order.order.domain.order.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "combination_lines")
public class CombinationLine {
    @JsonIgnore
    @Setter(AccessLevel.PACKAGE)
    @ManyToOne
    @JoinColumn(name = "storeId", referencedColumnName = "storeId")
    @JoinColumn(name = "orderId", referencedColumnName = "id")
    private Order aggRoot;

    @Id
    private int id;

    private int variantId;
    private int productId;

    private BigDecimal price;
    private BigDecimal quantity;

    private @Size(max = 320) String title;
    private @Size(max = 500) String variantTitle;
    private @Size(max = 850) String name;

    private @Size(max = 50) String sku;
    private @Size(max = 255) String vendor;
    private @Size(max = 50) String unit;
    private @Size(max = 50) String itemUnit;

    @Enumerated(EnumType.STRING)
    private Type type;

    public CombinationLine(
            int id,
            int variantId,
            int productId,
            BigDecimal price,
            BigDecimal quantity,
            String title,
            String variantTitle,
            String sku,
            String vendor,
            String unit,
            String itemUnit,
            Type type
    ) {
        this.id = id;
        this.variantId = variantId;
        this.productId = productId;
        this.price = price;
        this.quantity = quantity;
        this.sku = sku;
        this.vendor = vendor;
        this.unit = unit;
        this.itemUnit = itemUnit;
        this.type = type;
        initLineName(title, variantTitle);
    }

    private void initLineName(String title, String variantTitle) {
        this.title = title;
        this.variantTitle = variantTitle;
        StringBuilder nameBuilder = new StringBuilder(title);
        if (StringUtils.isNotBlank(variantTitle) && !"Default Title".equals(variantTitle)) {
            nameBuilder.append(" - ").append(variantTitle);
        }
        this.name = nameBuilder.toString();
    }

    public enum Type {
        combo,
        packsize
    }
}
