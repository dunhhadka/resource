package com.example.storesecurity.configuration;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class RedisProperties {
    private String host;
    private int port = 6379;
    private String password;
    private int database = 0;
    private int timeout = 2000;

    private JedisPool pool;
}
