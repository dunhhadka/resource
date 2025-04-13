package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.ddd.ValueObject;

import java.math.BigDecimal;

@Getter
@Builder(toBuilder = true)
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class VariantPricingInfo extends ValueObject<VariantPricingInfo> {
    @Builder.Default
    @DecimalMin(value = "0")
    @DecimalMax(value = "100000000000000")
    private BigDecimal price = BigDecimal.ZERO;
    @DecimalMin(value = "0")
    @DecimalMax(value = "100000000000000")
    private BigDecimal compareAtPrice;
    private boolean taxable;
}
