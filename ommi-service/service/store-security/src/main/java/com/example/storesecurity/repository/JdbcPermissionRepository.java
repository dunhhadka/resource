package com.example.storesecurity.repository;

import com.example.storesecurity.cache.LRUCache;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class JdbcPermissionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final LRUCache<String, Object> lruCache;

    public JdbcPermissionRepository(JdbcTemplate jdbcTemplate, LRUCache<String, Object> lruCache) {
        this.jdbcTemplate = jdbcTemplate;
        this.lruCache = lruCache;
    }

    public List<String> getPermissions(int storeId, int id) {
        return List.of();
    }
}
