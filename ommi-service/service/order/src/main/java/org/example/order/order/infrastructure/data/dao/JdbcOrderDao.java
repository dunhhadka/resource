package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.OrderDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcOrderDao implements OrderDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public OrderDto getByReference(int storeId, String reference) {
        var result = jdbcTemplate.query(
                """
                        SELECT TOP 1 * FROM orders
                        WHERE store_id = :storeId AND reference = :reference
                        AND status IN ('open', 'closed', 'cancelled')
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("reference", reference),
                BeanPropertyRowMapper.newInstance(OrderDto.class));
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public OrderDto getById(int storeId, int id) {
        var result = jdbcTemplate.query(
                """
                        SELECT TOP 1 * FROM orders
                        WHERE store_id = :storeId AND id = :id
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("id", id),
                BeanPropertyRowMapper.newInstance(OrderDto.class));
        return result.isEmpty() ? null : result.get(0);
    }

}
