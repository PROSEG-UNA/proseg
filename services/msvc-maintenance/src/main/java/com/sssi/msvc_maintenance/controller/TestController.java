package com.sssi.msvc_maintenance.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${routes.test}")
@RequiredArgsConstructor
public class TestController {

    @GetMapping
    public String hello() {
        return "HOLA CRACKS";
    }

}
