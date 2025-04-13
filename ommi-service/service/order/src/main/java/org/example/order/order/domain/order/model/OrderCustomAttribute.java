package org.example.order.order.domain.order.model;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Embeddable
public class OrderCustomAttribute {
    @NotBlank
    @Size(max = 250)
    private String name;

    @Size(max = 255)
    private String value;

    public OrderCustomAttribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public void updateValue(String value) {
        this.value = value;
    }
}
