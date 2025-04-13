package org.example.product.product.application.service.routing;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class FulfillmentGroup {
    @Builder.Default
    private UUID id = UUID.randomUUID();

    private final List<RoutingItem> items;

    private final List<Integer> locationIds;
}
