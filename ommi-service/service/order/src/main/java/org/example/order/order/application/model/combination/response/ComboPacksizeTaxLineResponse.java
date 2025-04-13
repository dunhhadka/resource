package org.example.order.order.application.model.combination.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ComboPacksizeTaxLineResponse {
    private String title;
    private BigDecimal rate;
    private BigDecimal price;
    private boolean custom;

    public ComboPacksizeTaxLineResponse addPrice(BigDecimal price) {
        this.price = this.price.add(price);
        return this;
    }
}
