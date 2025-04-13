package org.example.product.product.domain.product.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.product.ddd.ValueObject;

@Getter
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class ImagePhysicalInfo extends ValueObject<ImagePhysicalInfo> {
    private int size;
    private Integer width;
    private Integer height;
}
