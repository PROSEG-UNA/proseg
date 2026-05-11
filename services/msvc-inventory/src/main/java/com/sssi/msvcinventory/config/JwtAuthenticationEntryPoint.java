package com.sssi.msvcinventory.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sssi.common.api.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException ex
    ) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse error = new ApiErrorResponse(
                resolveMessage(ex),
                List.of(ex.getMessage() != null ? ex.getMessage() : "Unauthorized"),
                HttpStatus.UNAUTHORIZED.value()
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String resolveMessage(AuthenticationException ex) {
        String cause = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        if (cause.contains("expired")) return "Token expirado";
        if (cause.contains("invalid") || cause.contains("malformed")) return "Token inválido";
        if (cause.contains("signature")) return "Firma inválida";
        return "No autenticado";
    }
}
