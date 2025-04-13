package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.ddd.ValueObject;

@Getter
@Builder(toBuilder = true)
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class VariantIdentityInfo extends ValueObject<VariantIdentityInfo> {
    @Size(max = 50)
    private String barcode;
    @Size(max = 50)
    private String sku;
}
