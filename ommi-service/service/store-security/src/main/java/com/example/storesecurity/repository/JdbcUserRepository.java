package com.example.storesecurity.repository;

import com.example.storesecurity.cache.LRUCache;
import com.example.storesecurity.dto.UserDto;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcUserRepository {
    private static final String USER_GET_BY_STORE_ID_AND_PHONE_KEY_PATTERN = "%d::users.%s";
    private static final String USER_GET_BY_STORE_ID_AND_EMAIL_KEY_PATTERN = "%d::users.%s";
    private static final String USER_GET_BY_STORE_ID_AND_ID_PATTERN = "%d::users.%s";

    private final LRUCache<String, Object> lruCache;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcUserRepository(NamedParameterJdbcTemplate jdbcTemplate, LRUCache<String, Object> lruCache) {
        this.lruCache = lruCache;
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserDto getUserByEmail(int storeId, String email) {
        var cacheKey = USER_GET_BY_STORE_ID_AND_EMAIL_KEY_PATTERN.formatted(storeId, email);
        return (UserDto) lruCache.getOrInsert(cacheKey, () -> {
            var result = this.jdbcTemplate.query(
                    """
                            SELECT * FROM users WHERE store_id = :storeId AND email = :email
                            """,
                    new MapSqlParameterSource()
                            .addValue("storeId", storeId)
                            .addValue("email", email),
                    BeanPropertyRowMapper.newInstance(UserDto.class)
            );
            return result.isEmpty() ? null : result.get(0);
        });
    }

    public UserDto getUserByPhone(int storeId, String username) {
        return null;
    }
}
