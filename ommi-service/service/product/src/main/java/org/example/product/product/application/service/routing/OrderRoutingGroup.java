package org.example.product.product.application.service.routing;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Kết quả sau khi tính toán
 * - locationId: location tính toán được
 * - itemIndexes: location có thể routing được đến với item_index nào
 */
@Getter
@Builder
public class OrderRoutingGroup {
    private final int locationId;
    private final List<Integer> itemIndexes;
}
