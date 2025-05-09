package com.example.storesecurity.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.List;

@Slf4j
public final class StoreAuthenticationProvider implements AuthenticationProvider {

    private final List<PrincipalResolver> principalResolvers;

    public StoreAuthenticationProvider(List<PrincipalResolver> principalResolvers) {
        this.principalResolvers = principalResolvers;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!(authentication instanceof StoreAuthenticationToken token)) {
            return null;
        }

        for (var repository : principalResolvers) {
            if (repository.support(token)) {
                log.debug("{} resolve token ", repository.getClass().getSimpleName());
//                var principal = repository.resolve(token);
//                log.debug("principal {} ", principal);
                return new StoreAuthenticationToken(null, null);
            }
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return StoreAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
