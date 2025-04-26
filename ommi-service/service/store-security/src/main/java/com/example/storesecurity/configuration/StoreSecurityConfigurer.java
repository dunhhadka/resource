package com.example.storesecurity.configuration;

import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public final class StoreSecurityConfigurer extends AbstractHttpConfigurer<StoreSecurityConfigurer, HttpSecurity> {

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final StoreAuthenticationFilter filter;

    private final ApplicationContext applicationContext;

    public StoreSecurityConfigurer(
            StoreAuthenticationFilter filter,
            ApplicationContext applicationContext
    ) {
        this.applicationContext = applicationContext;
        this.authenticationEntryPoint = new StoreAuthenticationEntryPoint();
        this.accessDeniedHandler = new StoreAccessDeniedHandler();
        this.filter = filter;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterBefore(filter, BasicAuthenticationFilter.class);
    }

    private AuthenticationManager getAuthenticationManager(HttpSecurity http) {
        return http.getSharedObject(AuthenticationManager.class);
    }
}
