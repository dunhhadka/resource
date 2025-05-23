package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcOrderEditDao implements OrderEditDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public OrderEditDto getById(int storeId, int id) {
        var result = jdbcTemplate.query(
                """
                        SELECT * FROM order_edits
                        WHERE store_id = :storeId AND id = :id
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("id", id),
                BeanPropertyRowMapper.newInstance(OrderEditDto.class)
        );
        return result.isEmpty() ? null : result.get(0);
    }
}
