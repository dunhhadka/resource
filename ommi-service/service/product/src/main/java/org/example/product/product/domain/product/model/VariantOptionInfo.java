package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
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
public class VariantOptionInfo extends ValueObject<VariantOptionInfo> {
    public static final String DEFAULT_OPTION_VALUE = "Default Title";

    @NotBlank
    @Size(max = 500)
    private String option1 = DEFAULT_OPTION_VALUE;

    @Size(max = 500)
    private String option2;

    @Size(max = 500)
    private String option3;
}
