package org.example.order.order.domain.draftorder.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class DraftOrderId implements Serializable {
    private int storeId;
    private int id;
}
