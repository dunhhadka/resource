package org.example.inventory;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InventoryRequest {
    private String idempotencyKey;
    private List<AdjustmentRequest> adjustments;
    private Long actorId;
    private String actorName;
    private String costType;
    private String behavior;
}
