package com.example.storesecurity.configuration;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

public class StoreSecurityProvider implements AuthenticationProvider {
    private final List<PrincipalResolver> resolvers;

    public StoreSecurityProvider(List<PrincipalResolver> resolvers) {
        this.resolvers = resolvers;
        System.out.println("StoreSecurityProvider initialized with resolvers: " + resolvers);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        System.out.println("Authenticating: " + authentication);
        
        if (!(authentication instanceof StoreAuthenticationToken)) {
            return null;
        }
        
        StoreAuthenticationToken token = (StoreAuthenticationToken) authentication;
        
        // Try to resolve principal using all resolvers
        for (PrincipalResolver resolver : resolvers) {
            if (resolver.support(token)) {
                StoreAuthenticatedPrincipal principal = resolver.resolve(token);
                if (principal != null) {
                    // Create authenticated token with principal
                    token.setAuthenticated(true);
                    return token;
                }
            }
        }
        
        return null; // Return null if no resolver could authenticate
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return StoreAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
