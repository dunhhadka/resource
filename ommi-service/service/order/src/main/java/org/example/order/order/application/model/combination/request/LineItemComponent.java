package org.example.order.order.application.model.combination.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.order.order.domain.draftorder.model.VariantType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Builder
public class LineItemComponent {
    private int variantId;
    private int productId;

    private Integer inventoryItemId;

    private String sku;
    private String title;
    private String variantTitle;

    private String vendor;
    private String unit;

    private String inventoryManagement;
    private String inventoryPolicy;

    private int grams;
    private boolean requireShipping;
    private boolean taxable;

    @NotNull
    private BigDecimal quantity;

    // Số lượng sản phẩm tính trong 1 combo/packsize
    private BigDecimal baseQuantity;

    // Giá của sản phẩm trong 1 combo/packsize
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    // Giá của toàn line thành phần sau
    private BigDecimal linePrice;

    @Builder.Default
    private List<ComboPacksizeDiscountAllocations> discountAllocations = new ArrayList<>();

    @Builder.Default
    private List<ComboPacksizeTaxLine> taxLines = new ArrayList<>();

    private VariantType type;

    private boolean canBeOdd;
}
