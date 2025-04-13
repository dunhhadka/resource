package org.example.order.order.domain.order.rule;

import org.example.order.ddd.DomainRule;

public class FulfillLineItemRule implements DomainRule {

    private final int fulfillableQuantity;
    private final int fulfillQuantity;

    public FulfillLineItemRule(int fulfillableQuantity, int fulfillQuantity) {
        this.fulfillableQuantity = fulfillableQuantity;
        this.fulfillQuantity = fulfillQuantity;
    }

    @Override
    public boolean isBroken() {
        return fulfillQuantity > fulfillableQuantity;
    }

    @Override
    public String message() {
        return "The quantity to fulfill must be smaller than or equal to " + fulfillableQuantity;
    }
}
