package org.example.product.product.domain.product.rules;

import org.example.product.ddd.DomainRule;

public class ProductLimitTag implements DomainRule {

    private static final int TAX_LIMIT = 100;

    private final int tagCount;

    public ProductLimitTag(int tagCount) {
        this.tagCount = tagCount;
    }

    @Override
    public boolean isBroken() {
        return tagCount > TAX_LIMIT;
    }

    @Override
    public String message() {
        return null;
    }
}
