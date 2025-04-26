package com.example.storesecurity.configuration;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StoreAuthenticationToken extends AbstractAuthenticationToken {

    @Getter
    private final String token;
    private Object credentials;
    private Map<String, Object> attributes = new HashMap<>();

    public StoreAuthenticationToken(String token, Map<String, Object> attributes) {
        super(Collections.emptyList());
        this.token = token;
        this.attributes = attributes;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.token;
    }
}
