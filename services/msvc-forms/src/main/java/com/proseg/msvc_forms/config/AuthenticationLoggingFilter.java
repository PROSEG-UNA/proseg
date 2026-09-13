package com.proseg.msvc_forms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class AuthenticationLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        if (!request.getRequestURI().startsWith("/api/v1/forms")) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<String> authorities = authentication == null
                ? List.of()
                : authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .sorted()
                .toList();

        log.info("Forms request path={} principal={} authorities={} status={}",
                request.getRequestURI(),
                authentication == null ? "anonymous" : authentication.getName(),
                authorities,
                response.getStatus());
    }
}
