package com.example.storesecurity.configuration;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

public final class StoreAuthenticationProvider implements AuthenticationProvider {

    private final List<PrincipalResolver> principalResolvers;

    public StoreAuthenticationProvider(List<PrincipalResolver> principalResolvers) {
        this.principalResolvers = principalResolvers;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return false;
    }
}
