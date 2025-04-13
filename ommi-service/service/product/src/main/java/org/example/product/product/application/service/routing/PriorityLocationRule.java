package org.example.product.product.application.service.routing;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class PriorityLocationRule extends RankingLocationRule {

    private final List<LocationInfo> availableLocations;

    public PriorityLocationRule(List<LocationInfo> availableLocations) {
        this.availableLocations = availableLocations;
    }

    @Override
    public List<RankingLocationResult> process(List<FulfillmentGroup> fulfillmentGroups) {
        var rankSortedLocations = sortRank(this.availableLocations);

        var result = new ArrayList<RankingLocationResult>();
        for (var fulfillmentGroup : fulfillmentGroups) {
            var locations = ranking(rankSortedLocations, fulfillmentGroup.getLocationIds());
            result.add(new RankingLocationResult(fulfillmentGroup.getId().toString(), locations));
        }
        return result;
    }

    private List<Pair<Integer, Integer>> ranking(List<LocationInfo> rankSortedLocations, List<Integer> locationIds) {
        var tmpResult = new ArrayList<Pair<Integer, Integer>>();
        for (int i = 0; i < rankSortedLocations.size(); i++) {
            var location = rankSortedLocations.get(i);
            if (!locationIds.contains(location.getId())) {
                continue;
            }
            tmpResult.add(Pair.of(location.getId(), i));
        }

        return tmpResult;
    }

    private List<LocationInfo> sortRank(List<LocationInfo> availableLocations) {
        return availableLocations.stream()
                .sorted((o1, o2) -> {
                    if (o1.getRank() == null && o2.getRank() == null) {
                        return o1.getId() - o2.getId();
                    } else if (o1.getRank() == null) {
                        return 1;
                    } else if (o2.getRank() == null) {
                        return -1;
                    } else {
                        return o1.getRank() - o2.getRank();
                    }
                })
                .toList();
    }
}
