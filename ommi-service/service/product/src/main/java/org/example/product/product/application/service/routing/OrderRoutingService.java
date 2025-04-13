package org.example.product.product.application.service.routing;

import lombok.RequiredArgsConstructor;
import org.example.product.product.application.model.product.OrderRoutingItemResponse;
import org.example.product.product.application.model.product.OrderRoutingRequest;
import org.example.product.product.application.model.product.OrderRoutingResultResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderRoutingService {

    private final OrderRoutingDataService routingDataService;

    public List<OrderRoutingResultResponse> process(int storeId, OrderRoutingRequest orderRoutingRequest) {
        // Tách riêng loại item
        // Sử dụng index để tính toán location tối ưu
        List<RoutingItem> routingItems = new ArrayList<>();
        for (int i = 0; i < orderRoutingRequest.getItems().size(); i++) {
            var itemRequest = orderRoutingRequest.getItems().get(i);
            if (itemRequest.getVariantId() == null) {
                routingItems.add(new OrderRoutingCustomItem(i, itemRequest.getQuantity(), itemRequest.isRequireShipping()));
            } else {
                routingItems.add(new OrderRoutingNormalItem(i, itemRequest.getQuantity(), itemRequest.getVariantId()));
            }
        }

        var availableLocations = this.routingDataService.getAvailableLocations(storeId);

        var orderRoutingFetcher = new OrderRoutingFetcher(
                storeId,
                routingItems,
                availableLocations,
                orderRoutingRequest.getBehavior(),
                orderRoutingRequest.getShippingAddress(),
                routingDataService
        );

        var routingProcessResult = orderRoutingFetcher.process();
        var response = new ArrayList<OrderRoutingResultResponse>();
        for (var routingGroup : routingProcessResult.getRoutingGroups()) {
            var resultResponse = new OrderRoutingResultResponse();
            resultResponse.setLocationId(routingGroup.getLocationId());
            for (var itemIndex : routingGroup.getItemIndexes()) {
                var item = orderRoutingFetcher.getItems().get(itemIndex);
                if (item instanceof OrderRoutingNormalItem normalItem) {
                    resultResponse.getItems().add(new OrderRoutingItemResponse(itemIndex, normalItem.getInventoryItem().getId()));
                } else {
                    resultResponse.getItems().add(new OrderRoutingItemResponse(itemIndex));
                }
            }
        }
        return response;
    }
}
