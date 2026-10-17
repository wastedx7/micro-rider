package com.micro.api_gateway.config;

import java.time.Duration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Redis-backed login limiter shared by all gateway instances. */
@Component
public class LoginRateLimitFilter implements GlobalFilter, Ordered {
    private static final long MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final ReactiveStringRedisTemplate redis;

    public LoginRateLimitFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        if (!"/auth/login".equals(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        String address = exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        String key = "rate-limit:auth-login:" + address;
        return redis.opsForValue().increment(key)
                .flatMap(attempts -> {
                    Mono<Boolean> expiry = attempts == 1
                            ? redis.expire(key, WINDOW)
                            : Mono.just(true);
                    return expiry.then(Mono.defer(() -> {
                        if (attempts > MAX_ATTEMPTS) {
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            exchange.getResponse().getHeaders().set("Retry-After", "60");
                            return exchange.getResponse().setComplete();
                        }
                        return chain.filter(exchange);
                    }));
                })
                .onErrorResume(error -> chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
