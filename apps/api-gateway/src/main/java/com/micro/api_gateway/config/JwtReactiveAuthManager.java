package com.micro.api_gateway.config;

import java.util.List;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.micro.api_gateway.service.JwtService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtReactiveAuthManager implements ReactiveAuthenticationManager {
    
    private final JwtService jwtService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication){
        String token = authentication.getCredentials().toString();
        if(!jwtService.isTokenValid(token)){
            return Mono.error(new BadCredentialsException("invalid jwt token"));
        }
        String userId = jwtService.extractUUID(token);
        List<SimpleGrantedAuthority> authorities = jwtService.extractRoles(token).stream()
            .map(SimpleGrantedAuthority::new)
            .toList();

        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        return Mono.just(auth);
    }

}
