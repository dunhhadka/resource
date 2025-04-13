package org.example.order.order.application.model.orderedit.response;

import lombok.Getter;
import lombok.Setter;
import org.example.order.order.application.service.orderedit.GenericTaxLine;
import org.example.order.order.application.service.orderedit.MergedTaxLine;

import java.math.BigDecimal;

@Getter
@Setter
public class CalculatedTaxLine {
    private String title;
    private BigDecimal price;
    private BigDecimal rate;
    private BigDecimal ratePercentage;

    private BigDecimal quantity;
    private boolean custom;

    public CalculatedTaxLine(GenericTaxLine taxLine) {
        this.title = taxLine.getTitle();
        this.price = taxLine.getPrice();
        this.rate = taxLine.getRate();

        this.quantity = taxLine.getQuantity();
        this.custom = taxLine.isCustom();
    }
}
