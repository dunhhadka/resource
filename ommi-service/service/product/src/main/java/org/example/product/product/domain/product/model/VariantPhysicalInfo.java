package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.ddd.ValueObject;
import org.example.product.product.application.common.StringInList;

@Getter
@Builder(toBuilder = true)
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class VariantPhysicalInfo extends ValueObject<VariantPhysicalInfo> {

    @Builder.Default
    private boolean requireShipping = true;

    private double weight = 0;

    @Builder.Default
    @Size(max = 20)
    @StringInList(array = {"kg", "lb", "oz", "g"})
    private String weighUnit = "kg";

    @Size(max = 50)
    private String unit;
}
