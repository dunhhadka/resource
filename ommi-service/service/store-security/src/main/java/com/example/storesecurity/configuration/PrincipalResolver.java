package com.example.storesecurity.configuration;

public interface PrincipalResolver {
    StoreAuthenticatedPrincipal resolve(StoreAuthenticationToken token);

    boolean support(StoreAuthenticationToken token);
}
