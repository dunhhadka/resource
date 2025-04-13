package org.example.order.order.application.model.combination.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ComboPacksizeTaxLine {
    @NotBlank
    private String title;

    @NotNull
    private BigDecimal price;

    private BigDecimal rate;

    private boolean custom;

    public ComboPacksizeTaxLine addPrice(BigDecimal price) {
        this.price = this.price.add(price);
        return this;
    }
}
