package org.example.order.ddd;

public abstract class NestedDomainEntity<R extends AggregateRoot<R>> extends DomainEntity<R> {
    protected abstract R getAggRoot();

    protected void addDomainEvent(DomainEvent event) {
        this.getAggRoot().addEvent(event);
    }
}
