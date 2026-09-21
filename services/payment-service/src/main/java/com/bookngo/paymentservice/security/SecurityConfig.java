package com.bookngo.paymentservice.security;

import java.time.OffsetDateTime;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.bookngo.paymentservice.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ProviderSignatureFilter providerSignatureFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ProviderSignatureFilter providerSignatureFilter,
            ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.providerSignatureFilter = providerSignatureFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(OffsetDateTime.now())
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                    .code("UNAUTHORIZED")
                    .message(authException.getMessage() != null && !authException.getMessage().isEmpty()
                            ? authException.getMessage()
                            : "Missing or invalid authentication token")
                    .path(request.getRequestURI())
                    .build();
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .timestamp(OffsetDateTime.now())
                    .status(HttpStatus.FORBIDDEN.value())
                    .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                    .code("FORBIDDEN")
                    .message(accessDeniedException.getMessage() != null && !accessDeniedException.getMessage().isEmpty()
                            ? accessDeniedException.getMessage()
                            : "Insufficient permissions")
                    .path(request.getRequestURI())
                    .build();
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        // Provider callback: authenticated by ProviderSignatureFilter; permit here,
                        // the filter sets SecurityContext so @PreAuthorize works if needed.
                        .requestMatchers("/api/v1/payments/provider/callback").permitAll()
                        // Internal endpoints protected by JWT (ADMIN role service-to-service token)
                        .requestMatchers("/internal/v1/payments/**").authenticated()
                        // All public payment APIs require authentication
                        .requestMatchers("/api/v1/payments/**").authenticated()
                        .anyRequest().authenticated()
                )
                // ProviderSignatureFilter must run BEFORE JwtAuthenticationFilter to set auth for callback path
                .addFilterBefore(providerSignatureFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
