package org.example.order.order.infrastructure.data.dao;

import lombok.RequiredArgsConstructor;
import org.example.order.order.infrastructure.data.dto.ProductDto;
import org.example.order.order.infrastructure.data.dto.VariantDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcProductDao implements ProductDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<ProductDto> findProductByListId(int storeId, List<Integer> productIds) {
        return jdbcTemplate.query(
                """
                        SELECT * FROM products WHERE storeId = :storeId AND id IN :productIds
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("productIds", productIds),
                BeanPropertyRowMapper.newInstance(ProductDto.class)
        );
    }

    @Override
    public List<VariantDto> findVariantByListId(int storeId, List<Integer> variantIds) {
        return jdbcTemplate.query(
                """
                        SELECT * FROM variants WHERE storeId = :storeId AND id IN :variantIds
                        """,
                new MapSqlParameterSource()
                        .addValue("storeId", storeId)
                        .addValue("variantIds", variantIds),
                BeanPropertyRowMapper.newInstance(VariantDto.class)
        );
    }
}
