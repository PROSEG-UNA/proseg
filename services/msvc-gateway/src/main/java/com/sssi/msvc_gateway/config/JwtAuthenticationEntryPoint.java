package com.sssi.msvc_gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sssi.common.api.response.ApiErrorResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

public class JwtAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JwtAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiErrorResponse response = new ApiErrorResponse(
                resolveMessage(ex),
                List.of(ex.getMessage() != null ? ex.getMessage() : "Unauthorized"),
                HttpStatus.UNAUTHORIZED.value()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse()
                    .bufferFactory()
                    .wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return exchange.getResponse().setComplete();
        }
    }

    private String resolveMessage(AuthenticationException ex) {
        String cause = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        if (cause.contains("expired")) return "Token expirado";
        if (cause.contains("invalid") || cause.contains("malformed")) return "Token inválido";
        if (cause.contains("signature")) return "Firma del token inválida";
        return "No autenticado";
    }
}