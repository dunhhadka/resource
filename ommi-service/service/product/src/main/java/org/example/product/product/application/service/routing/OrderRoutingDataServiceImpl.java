package org.example.product.product.application.service.routing;

import lombok.RequiredArgsConstructor;
import org.example.product.product.application.model.product.OrderRoutingRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderRoutingDataServiceImpl implements OrderRoutingDataService {
    private final NamedParameterJdbcTemplate jdbcTemplate;


    @Override
    public List<LocationInfo> getAvailableLocations(int storeId) {
        return this.jdbcTemplate.query(
                """
                        SELECT 
                            id, 
                            rank
                        FROM store_shipping_addresses
                        WHERE 
                            store_id = :storeId
                            AND deleted = 0
                            AND inventory_management = 1
                            AND storage = 1
                         """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId),
                (rs, rowNum) -> LocationInfo.builder()
                        .id(rs.getInt(1))
                        .rank(rs.getInt(2))
                        .build()
        );
    }

    @Override
    public List<VariantInfo> getVariantInfos(int storeId, List<Integer> variantIds) {
        return this.jdbcTemplate.query(
                """
                        SELECT 
                            id,
                            inventory_item_id,
                            inventory_policy     
                        FROM variants
                        WHERE
                            store_id = :storeId
                            AND variant_id IN (:variantIds)
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("variantIds", variantIds),
                (rs, rowNum) -> VariantInfo.builder()
                        .id(rs.getInt(1))
                        .inventoryItemId(rs.getInt(2))
                        .inventoryPolicy(rs.getString(3))
                        .build()
        );
    }

    @Override
    public List<InventoryItemInfo> getInventoryItemInfos(int storeId, List<Integer> inventoryItemIds, List<Integer> variantIds) {
        return List.of();
    }

    @Override
    public List<InventoryLevelInfo> getInventoryLevelInfos(int storeId, List<Integer> inventoryItemIds, List<Integer> availableLocationIds) {
        return List.of();
    }

    @Override
    public Boolean checkShippingAddressCanFulfill(int storeId, OrderRoutingRequest.ShippingAddress shippingAddress) {
        return null;
    }
}

