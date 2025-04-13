package org.example.product.product.application.service.routing;

import com.google.common.base.Suppliers;
import org.apache.commons.lang3.StringUtils;
import org.example.product.product.application.model.product.OrderRoutingRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class OrderRoutingFetcher {

    private final int storeId;
    private final List<RoutingItem> routingItems;
    private final List<LocationInfo> availableLocations;
    final Behavior behavior;
    private final OrderRoutingRequest.ShippingAddress shippingAddress;

    private final OrderRoutingDataService dataService;

    private final Supplier<List<VariantInfo>> getVariantsSupplier;
    private final Supplier<List<InventoryItemInfo>> getInventoryItemsSupplier;
    private final Supplier<List<InventoryLevelInfo>> getInventoryLevelsSupplier;

    private final Supplier<Boolean> checkShippingAddressCanFulfillSupplier;

    private final List<RankingLocationRule> rankingLocationRules;

    public OrderRoutingFetcher(
            int storeId,
            List<RoutingItem> routingItems,
            List<LocationInfo> availableLocations,
            Behavior behavior,
            OrderRoutingRequest.ShippingAddress shippingAddress,
            OrderRoutingDataService routingDataService
    ) {
        this.storeId = storeId;

        this.routingItems = routingItems;

        this.availableLocations = availableLocations;

        this.behavior = behavior;
        this.shippingAddress = shippingAddress;

        this.dataService = routingDataService;

        this.getVariantsSupplier = Suppliers.memoize(() -> this.dataService.getVariantInfos(this.storeId, getVariantIds()));
        this.getInventoryItemsSupplier = Suppliers.memoize(() -> this.dataService.getInventoryItemInfos(this.storeId, getInventoryItemIds(), getVariantIds()));
        this.getInventoryLevelsSupplier = Suppliers.memoize(() -> this.dataService.getInventoryLevelInfos(storeId, getInventoryItemIds(), getAvailableLocationIds()));

        this.checkShippingAddressCanFulfillSupplier = Suppliers.memoize(() -> this.dataService.checkShippingAddressCanFulfill(storeId, this.shippingAddress));

        this.rankingLocationRules = List.of(
                new EnoughAvailableLocationRule(),
                new PriorityLocationRule(this.availableLocations)
        );
    }

    private List<Integer> getAvailableLocationIds() {
        return this.availableLocations.stream()
                .map(LocationInfo::getId)
                .distinct()
                .toList();
    }

    private List<Integer> getInventoryItemIds() {
        return this.routingItems.stream()
                .filter(OrderRoutingNormalItem.class::isInstance)
                .map(item -> ((OrderRoutingNormalItem) item).getVariantInfo().getInventoryItemId())
                .distinct()
                .toList();
    }

    private List<Integer> getVariantIds() {
        return this.routingItems.stream()
                .filter(OrderRoutingNormalItem.class::isInstance)
                .map(item -> ((OrderRoutingNormalItem) item).getVariantId())
                .distinct()
                .toList();
    }

    public VariantInfo getVariantInfo(OrderRoutingNormalItem orderRoutingNormalItem) {
        return getVariantsSupplier.get()
                .stream()
                .filter(v -> v.getId() == orderRoutingNormalItem.getVariantId())
                .findFirst()
                .orElseThrow();
    }

    public OrderRoutingResult process() {
        //Tạo ma trận location_index-item_index
        var matrix = createMatrix();
        // Xác định fulfillmentGroups
        var fulfillmentGroups = matrix.groups();

        if (fulfillmentGroups.isEmpty()) {
            return new OrderRoutingResult(List.of());
        }

        List<RankingLocationResult> rankingResults = new ArrayList<>();
        for (var rule : rankingLocationRules) {
            rankingResults.addAll(rule.process(fulfillmentGroups));
        }

        var group = fulfillmentGroups.get(0);

        var locationId = chooseLocationForGroup(group, rankingResults);
        var routingGroup = OrderRoutingGroup.builder()
                .locationId(locationId)
                .itemIndexes(group.getItems().stream().map(s -> s.index).toList())
                .build();
        return new OrderRoutingResult(List.of(routingGroup));
    }

    private int chooseLocationForGroup(FulfillmentGroup group, List<RankingLocationResult> rankingResults) {
        var rankingForGroup = rankingResults.stream()
                .filter(r -> StringUtils.equals(r.getFulfillmentGroupId(), group.getId().toString()))
                .toList();

        List<Integer> resultLocationIds = new ArrayList<>();
        for (var rankingLocationResult : rankingForGroup) {
            resultLocationIds = rankingLocationResult.getHighestRankLocations(resultLocationIds);
        }
        return resultLocationIds.get(0);
    }

    private OrderRoutingMatrix createMatrix() {
        var matrix = new OrderRoutingMatrix(this.availableLocations, this.routingItems);
        for (var item : routingItems) {
            var locationIds = item.detectLocationCanFulfill(availableLocations);
            for (var locationId : locationIds) {
                matrix.markCanFulfill(locationId, item.index);
            }
        }
        return matrix;
    }

    public Stream<InventoryLevelInfo> getInventoryLevels(OrderRoutingNormalItem normalItem) {
        return this.getInventoryLevelsSupplier.get()
                .stream()
                .filter(l -> l.getVariantId() == normalItem.getVariantId());
    }

    public InventoryItemInfo getInventoryItem(OrderRoutingNormalItem normalItem) {
        return getInventoryItemsSupplier.get()
                .stream()
                .filter(i -> i.getVariantId() == normalItem.getVariantId())
                .findFirst()
                .orElseThrow();
    }

    public boolean checkShippingAddressCanFulfill() {
        if (shippingAddress == null) {
            return true;
        }
        return this.checkShippingAddressCanFulfillSupplier.get();
    }

    public List<RoutingItem> getItems() {
        return this.routingItems;
    }
}
