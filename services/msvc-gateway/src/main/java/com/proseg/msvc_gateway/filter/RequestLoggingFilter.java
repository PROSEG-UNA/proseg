package com.proseg.msvc_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@Order(-150)
public class RequestLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (!path.startsWith("/api/v1/forms")) {
            return chain.filter(exchange);
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .flatMap(authentication ->
                        logRequest(exchange, chain, path, authentication)
                )
                .switchIfEmpty(
                        logRequest(exchange, chain, path, null)
                );
    }

    private Mono<Void> logRequest(
            ServerWebExchange exchange,
            WebFilterChain chain,
            String path,
            Authentication authentication
    ) {
        log.info(
                "Gateway request start path={} principal={} authorities={}",
                path,
                authentication == null ? "anonymous" : authentication.getName(),
                extractAuthorities(authentication)
        );

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    HttpStatusCode statusCode =
                            exchange.getResponse().getStatusCode();

                    log.info(
                            "Gateway request end path={} status={}",
                            path,
                            statusCode == null ? "null" : statusCode.value()
                    );
                });
    }

    private List<String> extractAuthorities(Authentication authentication) {
        if (authentication == null) {
            return List.of();
        }

        return authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .sorted()
                .toList();
    }
}