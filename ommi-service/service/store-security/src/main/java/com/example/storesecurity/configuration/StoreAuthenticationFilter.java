package com.example.storesecurity.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class StoreAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("StoreAuthenticationFilter - Path: {}, Method: {}, Thread: {}, Session: {}", 
            request.getRequestURI(), 
            request.getMethod(),
            Thread.currentThread().getName(),
            request.getSession(false) != null ? request.getSession().getId() : "no-session");



        filterChain.doFilter(request, response);
    }
}
