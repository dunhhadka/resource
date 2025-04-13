package org.example.product.product.application.service.routing;

import java.util.List;

public abstract class RankingLocationRule {

    public abstract List<RankingLocationResult> process(List<FulfillmentGroup> fulfillmentGroups);
}
