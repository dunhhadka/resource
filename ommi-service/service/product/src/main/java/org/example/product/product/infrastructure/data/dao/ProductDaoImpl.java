package org.example.product.product.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductDaoImpl implements ProductDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Long countProductInStore(int storeId) {
        if (storeId != 0) return 100L; // TODO: auth(Hà Văn Dũng)
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM Products WHERE storeId = :storeId
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId),
                Long.class
        );
    }
}
