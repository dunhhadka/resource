package org.example.product.ddd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;

@MappedSuperclass
public class DomainEntity<R extends AggregateRoot<R>> {
    @Transient
    private boolean isNew = true;

    @JsonIgnore
    public boolean isNew() {
        return this.isNew;
    }

    @PrePersist
    @PostLoad
    public void markNotNew() {
        this.isNew = false;
    }

    protected void checkRule(String errorKey, DomainRule rule) {
        if (rule.isBroken()) {
            throw new IllegalArgumentException(rule.message());
        }
    }
}
