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
public class ProductGeneralInfo extends ValueObject<ProductGeneralInfo> {
    @Size(max = 320)
    private String metaTitle;
    @Size(max = 320)
    private String metaDescription;
    @Size(max = 255)
    private String templateLayout;
    @Size(max = 1000)
    private String summary;
    @Size(max = 255)
    private String vendor;
    @Size(max = 255)
    private String productType;
}
