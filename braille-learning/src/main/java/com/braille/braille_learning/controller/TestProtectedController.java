package com.braille.braille_learning.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestProtectedController {

    @GetMapping("/protected")
    public ResponseEntity<Map<String, Object>> testProtected(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Access granted to protected endpoint",
                "user", authentication != null ? authentication.getName() : "anonymous"
        ));
    }
}
