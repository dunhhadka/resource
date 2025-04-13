package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.ddd.ValueObject;

import java.math.BigDecimal;

@Embeddable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPricingInfo extends ValueObject<ProductPricingInfo> {
    @Builder.Default
    private BigDecimal priceMax = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal priceMin = BigDecimal.ZERO;
    @Builder.Default
    private boolean priceVaries = false;
    @Builder.Default
    private BigDecimal compareAtPriceMax = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal compareAtPriceMin = BigDecimal.ZERO;
    @Builder.Default
    private boolean compareAtPriceVaries = false;
}
