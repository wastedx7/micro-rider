package com.micro.api_gateway.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Small local safeguard for login abuse. Replace with a distributed limiter
 * when multiple gateway instances are deployed.
 */
@Component
public class LoginRateLimitFilter implements GlobalFilter, Ordered {
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        if (!"/auth/login".equals(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        String key = exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        Window window = windows.compute(key, (ignored, current) -> {
            long now = System.currentTimeMillis();
            if (current == null || now - current.startedAt >= WINDOW_MILLIS) {
                return new Window(now);
            }
            current.attempts.incrementAndGet();
            return current;
        });
        if (window.attempts.get() > MAX_ATTEMPTS) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().set("Retry-After", "60");
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private static final class Window {
        private final long startedAt;
        private final AtomicInteger attempts = new AtomicInteger(1);

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
