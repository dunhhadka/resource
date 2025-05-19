package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.example.order.order.infrastructure.data.dto.FulfillmentDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Repository
@RequiredArgsConstructor
public class JdbcFulfillmentDao implements FulfillmentDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public CompletableFuture<List<FulfillmentDto>> getFulfillmentByStoreIdsAndOrderIdsAsync(List<Integer> storeIds, List<Integer> orderIds) {
        if (CollectionUtils.isEmpty(storeIds) || CollectionUtils.isEmpty(orderIds)) {
            return CompletableFuture.completedFuture(List.of());
        }

        return CompletableFuture.supplyAsync(() ->
                jdbcTemplate.query(
                        """
                                SELECT * FROM fulfillments WHERE store_id IN (:storeIds) AND order_id IN (:orderIds)
                                """,
                        new MapSqlParameterSource()
                                .addValue("storeIds", storeIds)
                                .addValue("orderIds", orderIds),
                        BeanPropertyRowMapper.newInstance(FulfillmentDto.class)

                ));
    }

    @Override
    public List<FulfillmentDto> getFulfillmentByStoreIdsAndOrderIds(Set<Integer> storeIds, Set<Integer> orderIds) {
        return this.jdbcTemplate.query(
                """
                        SELECT * FROM fulfillments WHERE store_id IN (:storeIds) AND order_id IN (:orderIds)
                        """,
                new MapSqlParameterSource()
                        .addValue("storeIds", storeIds)
                        .addValue("orderIds", orderIds),
                BeanPropertyRowMapper.newInstance(FulfillmentDto.class)
        );
    }

}
