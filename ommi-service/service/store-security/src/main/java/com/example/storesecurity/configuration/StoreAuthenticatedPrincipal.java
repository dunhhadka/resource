package com.example.storesecurity.configuration;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

public class StoreAuthenticatedPrincipal implements AuthenticationPrincipal {

    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    private final String username;

    public StoreAuthenticatedPrincipal(Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes, String username) {
        this.authorities = authorities;
        this.attributes = attributes;
        this.username = username;
    }

    @Override
    public boolean errorOnInvalidType() {
        return false;
    }

    @Override
    public String expression() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return null;
    }
}
