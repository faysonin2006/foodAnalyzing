package com.authservice.controllers;

import com.authservice.dtos.UserAuthResponse;
import com.authservice.dtos.UserLoginRequest;
import com.authservice.dtos.UserRegistrationRequest;
import com.authservice.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserAuthResponse> register (@RequestBody @Valid UserRegistrationRequest
                                                                  userRegistrationRequest) {
        return ResponseEntity.ok(authService.register(userRegistrationRequest));
    }
    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@RequestBody @Valid UserLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserAuthResponse> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}
