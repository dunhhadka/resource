package org.example.product.product.application.service.routing;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class OrderRoutingResult {
    private final List<OrderRoutingGroup> routingGroups;
}
