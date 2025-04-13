package org.example.product.product.application.service.routing;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Objects;

@Getter
public class RankingLocationResult {
    private final String fulfillmentGroupId;
    private final List<Pair<Integer, Integer>> locationIds;

    public RankingLocationResult(String fulfillmentGroupId, List<Pair<Integer, Integer>> locationIds) {
        this.fulfillmentGroupId = fulfillmentGroupId;
        this.locationIds = locationIds;
    }

    public List<Integer> getHighestRankLocations(List<Integer> resultLocationIds) {
        var maxRank = locationIds.stream()
                .filter(id -> CollectionUtils.isEmpty(resultLocationIds) || resultLocationIds.contains(id))
                .map(Pair::getValue)
                .min(Integer::compare)
                .orElse(0);
        return locationIds.stream()
                .filter(r -> Objects.equals(r.getValue(), maxRank))
                .map(Pair::getKey)
                .toList();
    }
}
