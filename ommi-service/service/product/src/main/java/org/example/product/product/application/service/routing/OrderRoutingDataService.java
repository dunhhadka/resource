package org.example.product.product.application.service.routing;

import org.example.product.product.application.model.product.OrderRoutingRequest;

import java.util.List;

public interface OrderRoutingDataService {

    List<LocationInfo> getAvailableLocations(int storeId);

    List<VariantInfo> getVariantInfos(int storeId, List<Integer> variantIds);

    List<InventoryItemInfo> getInventoryItemInfos(int storeId, List<Integer> inventoryItemIds, List<Integer> variantIds);

    List<InventoryLevelInfo> getInventoryLevelInfos(int storeId, List<Integer> inventoryItemIds, List<Integer> availableLocationIds);

    Boolean checkShippingAddressCanFulfill(int storeId, OrderRoutingRequest.ShippingAddress shippingAddress);
}
