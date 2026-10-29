package com.bookngo.showservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InstanceIdentityFilter extends OncePerRequestFilter {

    @Value("${spring.application.name:SHOW-SERVICE}")
    private String applicationName;

    @Value("${server.port:8083}")
    private String serverPort;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        response.setHeader(
                "X-BookNGo-Instance",
                applicationName + ":" + serverPort
        );

        filterChain.doFilter(request, response);
    }
}
