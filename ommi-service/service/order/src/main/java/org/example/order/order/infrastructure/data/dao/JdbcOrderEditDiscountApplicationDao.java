package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcOrderEditDiscountApplicationDao implements OrderEditDiscountApplicationDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<OrderEditDiscountApplicationDto> getByOrderEditId(int storeId, int orderEditId) {
        return jdbcTemplate.query(
                """
                        SELECT * FROM order_edit_discount_applications
                        WHERE store_id = :storeId AND editing_id = :orderEditId
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("orderEditId", orderEditId),
                BeanPropertyRowMapper.newInstance(OrderEditDiscountApplicationDto.class)
        );
    }
}
