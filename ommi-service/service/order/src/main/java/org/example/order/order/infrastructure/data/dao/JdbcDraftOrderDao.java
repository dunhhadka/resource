package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.DraftOrderDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcDraftOrderDao implements DraftOrderDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<DraftOrderDto> getForReIndexES(int startId, int take) {
        return jdbcTemplate.query(
                """
                        SELECT * FROM draft_orders WHERE id > :startId ORDER BY id ASC LIMIT :take
                        """,
                new MapSqlParameterSource()
                        .addValue("startId", startId)
                        .addValue("take", take),
                BeanPropertyRowMapper.newInstance(DraftOrderDto.class)
        );
    }
}
