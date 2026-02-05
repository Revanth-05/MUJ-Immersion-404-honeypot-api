package com.scam.honeypot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "Honeypot API is running 🚀";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
