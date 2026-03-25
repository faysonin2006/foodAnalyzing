package com.authservice.controllers;

import com.authservice.controllers.api.AuthControllerApi;
import com.authservice.dtos.UserAuthResponse;
import com.authservice.dtos.UserLoginRequest;
import com.authservice.dtos.UserRegistrationRequest;
import com.authservice.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthControllerApi {

    private final AuthService authService;

    @Override
    @PostMapping("/register")
    public ResponseEntity<UserAuthResponse> register(@RequestBody @Valid UserRegistrationRequest userRegistrationRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(userRegistrationRequest));
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@RequestBody @Valid UserLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<UserAuthResponse> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}
