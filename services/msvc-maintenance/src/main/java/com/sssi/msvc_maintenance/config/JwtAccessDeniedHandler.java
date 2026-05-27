package com.sssi.msvc_maintenance.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sssi.common.api.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.List;

public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ex
    ) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse error = new ApiErrorResponse(
                "Acceso denegado",
                List.of(ex.getMessage() != null ? ex.getMessage() : "No tiene permisos"),
                HttpStatus.FORBIDDEN.value()
        );
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
