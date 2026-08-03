package com.micro.api_gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class UUIDHeaderGatewayFIlter extends AbstractGatewayFilterFactory<UUIDHeaderGatewayFIlter.Config>{
    
    public UUIDHeaderGatewayFIlter(){
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> {
                Authentication auth = ctx.getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String userId) {
                    ServerWebExchange mutated = exchange.mutate()
                        .request(r -> r.header("X-User-ID", userId))
                        .build();
                    return chain.filter(mutated);
                }
                return chain.filter(exchange);
            })
            .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }
    
    public static class Config{
    }
}
