package org.example.product.ddd;

public interface DomainRule {
    boolean isBroken();

    String message();
}
