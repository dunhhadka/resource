package org.example.order.order.application.service.orderedit;

import org.example.order.order.application.model.orderedit.response.CalculatedLineItem;

import java.util.Map;

public interface BuilderSteps {

    interface Builder {
        Result build();
    }

    interface Result {
        CalculatedLineItem lineItem();

        Map<MergedTaxLine.TaxLineKey, MergedTaxLine> taxLines();
    }

}
