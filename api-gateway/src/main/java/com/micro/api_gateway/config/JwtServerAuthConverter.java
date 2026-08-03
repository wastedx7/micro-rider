package com.micro.api_gateway.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class JwtServerAuthConverter implements ServerAuthenticationConverter{
    
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange){
        String token = extractToken(exchange.getRequest());
        if (token == null) {
            return Mono.empty();
        }
        return Mono.just(new UsernamePasswordAuthenticationToken(null, token));
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        var cookies = request.getCookies().getFirst("jwt");
        if (cookies != null) {
            return cookies.getValue();
        }

        return null;
    }
}
