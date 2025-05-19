package org.example.order.order.domain.order.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@Entity
@Table(name = "order_logs")
@NoArgsConstructor
@AllArgsConstructor
public class OrderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Positive
    private int storeId;

    @Positive
    private int orderId;

    @Lob
    @NotNull
    private String data;

    @NotNull
    private AppEventType eventType;

    @NotNull
    private Instant createdOn;
}
