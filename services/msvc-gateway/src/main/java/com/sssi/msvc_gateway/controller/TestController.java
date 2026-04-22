package com.sssi.msvc_gateway.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "Endpoint público";
    }

    @GetMapping("/authenticated")
    public String authenticated(@AuthenticationPrincipal Jwt jwt) {
        return "Logueado como: " + jwt.getSubject();
    }

    @GetMapping("/admin")
    public String adminOnly() {
        return "Solo ADMIN puede ver esto";
    }

    @GetMapping("/user")
    public String userOrAdmin() {
        return "USER o ADMIN pueden ver esto";
    }
}