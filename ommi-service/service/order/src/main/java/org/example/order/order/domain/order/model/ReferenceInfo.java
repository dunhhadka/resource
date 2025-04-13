package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ReferenceInfo {
    @Min(1)
    private int number;
    @Min(1)
    private int orderNumber;
    @NotBlank
    @Size(max = 50)
    private String name;
    @NotBlank
    @Size(max = 32)
    private String token;
}
