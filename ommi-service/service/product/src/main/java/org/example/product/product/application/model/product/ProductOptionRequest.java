package org.example.product.product.application.model.product;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductOptionRequest {
    private @Size(max = 255) String name;
}
