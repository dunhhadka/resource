package org.example.product.product.domain.inventory.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class InventoryTrackingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int storeId;

    private String idempotencyKey;

    private Instant createdAt;

    public enum ConfirmStatus {
        unconfirmed,
        completed,
        failed
    }
}
