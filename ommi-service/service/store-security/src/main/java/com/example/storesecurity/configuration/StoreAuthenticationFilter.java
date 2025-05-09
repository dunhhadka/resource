package com.example.storesecurity.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationEntryPoint authenticationEntryPoint;

    private final TokenResolver tokenResolver;

    private final AuthenticationManager authenticationManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("StoreAuthenticationFilter - Path: {}, Method: {}, Thread: {}, Session: {}",
                request.getRequestURI(),
                request.getMethod(),
                Thread.currentThread().getName(),
                request.getSession(false) != null ? request.getSession().getId() : "no-session");

        StoreAuthenticationToken authenticationToken;
        try {
            authenticationToken = tokenResolver.resolve(request);
        } catch (AuthenticationException exception) {
            this.authenticationEntryPoint.commence(request, response, exception);
            return;
        }

//        if (authenticationToken == null) {
//            filterChain.doFilter(request, response);
//            return;
//        }

        Authentication authentication = authenticationManager.authenticate(new StoreAuthenticationToken(null, null));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
