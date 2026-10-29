package com.bookngo.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class PerimeterSecurityFilter implements GlobalFilter, Ordered {

    public static final String INTERNAL_PATH_PREFIX = "/internal/";
    public static final String OPERATOR_ASSIGNED_THEATRES_HEADER = "X-Operator-Assigned-Theatres";
    public static final String INTERNAL_CALL_HEADER = "X-Internal-Call";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Block external perimeter requests attempting to reach internal-only routes
        if (path != null && (path.startsWith(INTERNAL_PATH_PREFIX) || path.contains(INTERNAL_PATH_PREFIX))) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }

        // 2. Defense-in-depth: strip client-supplied internal spoofing headers
        boolean containsSpoofedHeader = request.getHeaders().containsKey(OPERATOR_ASSIGNED_THEATRES_HEADER)
                || request.getHeaders().containsKey(INTERNAL_CALL_HEADER);

        if (containsSpoofedHeader) {
            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove(OPERATOR_ASSIGNED_THEATRES_HEADER);
                        headers.remove(INTERNAL_CALL_HEADER);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

