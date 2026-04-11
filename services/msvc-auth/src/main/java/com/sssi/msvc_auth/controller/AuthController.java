package com.sssi.msvc_auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${routes.auth}")
public class AuthController {
    @GetMapping
    public String hello() {
        return "HELLO";
    }
}
