package com.bookngo.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

class PerimeterSecurityFilterTest {

    private PerimeterSecurityFilter filter;

    @BeforeEach
    void setUp() {
        filter = new PerimeterSecurityFilter();
    }

    @Test
    @DisplayName("Gateway perimeter: External requests to /internal/** routes are blocked with 404")
    void testInternalRouteBlockedWith404() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/internal/v1/shows/123/cancellation-policy").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertFalse(chainCalled.get(), "Chain must not be called for blocked internal routes");
        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Gateway perimeter: Path containing /internal/ is blocked with 404")
    void testContainedInternalRouteBlockedWith404() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/internal/bookings/123").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertFalse(chainCalled.get(), "Chain must not be called for blocked internal routes");
        assertEquals(HttpStatus.NOT_FOUND, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Gateway perimeter: X-Operator-Assigned-Theatres and X-Internal-Call headers are stripped")
    void testInternalHeadersAreStripped() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/shows")
                .header("X-Operator-Assigned-Theatres", "theatre-uuid-1234")
                .header("X-Internal-Call", "true")
                .header("Authorization", "Bearer legitimate-user-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> passedExchange = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            passedExchange.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertNotNull(passedExchange.get(), "Downstream chain must be called for valid public routes");
        assertFalse(passedExchange.get().getRequest().getHeaders().containsKey("X-Operator-Assigned-Theatres"),
                "X-Operator-Assigned-Theatres header must be stripped at gateway perimeter");
        assertFalse(passedExchange.get().getRequest().getHeaders().containsKey("X-Internal-Call"),
                "X-Internal-Call header must be stripped at gateway perimeter");
        assertTrue(passedExchange.get().getRequest().getHeaders().containsKey("Authorization"),
                "Legitimate Authorization header must be preserved");
    }

    @Test
    @DisplayName("Gateway perimeter: Legitimate public request passes through without modifications")
    void testPublicRoutePassesThrough() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/movies")
                .header("Authorization", "Bearer valid-token")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertTrue(chainCalled.get(), "Public route must pass through to downstream chain");
    }
}

