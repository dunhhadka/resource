package com.example.storesecurity.configuration;

import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

public class StoreSecurityConfigurer extends AbstractHttpConfigurer<StoreSecurityConfigurer, HttpSecurity> {
    private final ApplicationContext context;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;

    public StoreSecurityConfigurer(ApplicationContext context) {
        this.context = context;
        this.authenticationEntryPoint = null;
        this.accessDeniedHandler = null;
    }
}
