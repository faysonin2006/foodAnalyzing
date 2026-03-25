package com.apigateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
public class FallBackController {

    @GetMapping("/fallback/auth")
    public ResponseEntity<List<String>> authFallBack() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonList("Authentication service is unavailable, please try again later"));
    }
    @GetMapping("/fallback/profiles")
    public ResponseEntity<List<String>> usersFallBack() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonList("User service is unavailable, please try again later"));
    }
    @GetMapping("/fallback/food")
    public ResponseEntity<List<String>> foodFallBack() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonList("AIImage service is unavailable, please try again later"));
    }
    @GetMapping("/fallback/recipe")
    public ResponseEntity<List<String>> recipeFallBack() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonList("Recipe service is unavailable, please try again later"));
    }
    @GetMapping("/fallback/recipedb")
    public ResponseEntity<List<String>> recipeBDFallBack() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonList("Recipe service is unavailable, please try again later"));
    }
}
