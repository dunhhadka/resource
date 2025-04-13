package org.example.product.ddd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AggregateRoot<R extends AggregateRoot<R>> extends DomainEntity<R> {

    @JsonIgnore
    protected List<DomainEvent> events;

    protected void addEvent(DomainEvent event) {
        if (this.events == null) this.events = new ArrayList<>();
        events.add(event);
    }

    protected void clearEvents() {
        if (this.events == null) {
            if (log.isDebugEnabled()) {
                log.debug("DomainEvent is empty");
            }
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Clearing DomainEvent");
            this.events.clear();
        }
    }

    public List<DomainEvent> getDomainEvents() {
        return this.events;
    }
}
