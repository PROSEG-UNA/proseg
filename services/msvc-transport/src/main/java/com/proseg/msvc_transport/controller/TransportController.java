package com.proseg.msvc_transport.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("${routes.transport:/api/v1/transport}")
public class TransportController {

    @Value("${spring.application.name:msvc-transport}")
    private String serviceName;

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "service", serviceName,
                "status", "UP"
        ));
    }
}