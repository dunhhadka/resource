package org.example.product.product.application.service.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "inventory_outbox_kafka_message")
public class InventoryOutboxMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private int storeId;

    private String topicName;

    private String messageKey;

    private String messageValue;

    @CreationTimestamp
    private Instant createdOn;
}
